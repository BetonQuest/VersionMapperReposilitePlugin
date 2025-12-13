package org.betonquest.reposilite.mapper.settings;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.Min;
import com.reposilite.storage.api.Location;
import org.betonquest.reposilite.api.validation.ValidationResult;
import org.betonquest.reposilite.api.validation.ValidationType;
import org.betonquest.reposilite.mapper.integration.VersionMapperFacade;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Artifact to be used in {@link com.reposilite.configuration.shared.api.SharedSettings}.
 * Represents an artifact as defined in the settings of reposilite.
 *
 * @param id           The ID of the entry
 * @param repository   The repository the artifact is sourced from
 * @param groupId      The groupId of the artifact
 * @param artifactId   The ID of the artifact
 * @param versionXPath The xpath leading to an artifact version in pom.xml
 */
@SuppressWarnings("PMD.ShortVariable")
@Doc(title = "Artifact", description = "An artifact to be considered for listing requests")
public record Artifact(
        @Min(min = 1) @Doc(title = "id", description = "The ID of the entry") String id,
        @Doc(title = "Repository", description = "The repository the artifact is sourced from") String repository,
        @Doc(title = "GroupId", description = "The groupId of the artifact") String groupId,
        @Doc(title = "ArtifactId", description = "The Id of the artifact") String artifactId,
        @Doc(title = "xPaths", description = "The xpaths leading to an artifact version in pom.xml") List<XPathEntry> versionXPath) {

    /**
     * Regex for artifactId to check naming conventions as defined by Apache Maven.
     */
    public static final String ARTIFACT_ID = "^[a-z0-9-]+$";

    /**
     * Regex for groupId to check naming conventions as defined by Apache Maven.
     */
    public static final String GROUP_ID = "^[a-z][a-z0-9_.]*$";

    private static boolean validateId(final String targetRegex, final String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        final Pattern pattern = Pattern.compile(targetRegex);
        final Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    /**
     * Returns the {@link Location} of the artifact with the given version and extension.
     * Used to generate a path directly to an artifact's entry and file.
     *
     * @param version   the version of the artifact as listed in maven's metadata
     * @param extension the extension file targeted in the artifact
     * @return the {@link Location} of the artifact without existence check
     */
    public Location versionedGav(final String version, final String extension) {
        return gav().resolve(version).resolve(artifactId() + "-" + version + "." + extension);
    }

    /**
     * Returns the raw/root {@link Location} of the artifact.
     *
     * @return the raw/root {@link Location} of the artifact
     */
    public Location gav() {
        if (groupId == null || artifactId == null) {
            return Location.empty();
        }
        if (groupId.isBlank() || artifactId.isBlank()) {
            return Location.empty();
        }
        final String combined = groupId().replace('.', '%') + '%' + artifactId();
        final int firstPartIndex = combined.indexOf('%');
        final String first = combined.substring(0, firstPartIndex);
        final String remaining = combined.substring(firstPartIndex + 1);
        return Location.of(Path.of(first, remaining.split("%")));
    }

    /**
     * Checks if the artifact exists in the given repository.
     *
     * @param facade the facade to use for existence check
     * @return the validation result
     */
    public ValidationResult validateExistence(final VersionMapperFacade facade) {
        final List<String> errors = new ArrayList<>();
        boolean error = false;
        if (!facade.isRepositoryKnown(repository())) {
            errors.add("Unknown repository: \"" + repository() + "\"");
            error = true;
        }
        if (!facade.hasArtifact(repository(), gav())) {
            errors.add("Artifact not found in path: \"" + repository() + "/" + gav() + "\"");
            error = true;
        }
        return new ValidationResult("Entry \"" + id() + "\"" + (error ? " has issues:" : "'s artifact can be found and accessed."), error ? ValidationType.ERROR : ValidationType.SUCCESS, errors);
    }

    /**
     * Checks if the artifact's naming conventions are valid.
     *
     * @return the validation result
     */
    public ValidationResult validateNamingConvention() {
        final List<String> errors = new ArrayList<>();
        final boolean validatedArtifactId = validateId(ARTIFACT_ID, artifactId());
        final boolean validatedGroupId = validateId(GROUP_ID, groupId());
        if (!validatedArtifactId || !validatedGroupId) {
            if (!validatedGroupId) {
                errors.add("Poor 'groupId': \"" + groupId() + "\"");
            }
            if (!validatedArtifactId) {
                errors.add("Poor 'artifactId': \"" + artifactId() + "\"");
            }
            return new ValidationResult("\"" + id() + "\": Apache Maven naming conventions for entry violated.", ValidationType.ERROR, errors);
        }
        return new ValidationResult("\"" + id() + "\"'s naming is valid.", ValidationType.SUCCESS, new ArrayList<>());
    }
}
