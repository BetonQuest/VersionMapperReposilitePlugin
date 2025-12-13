package org.betonquest.reposilite.mapper.settings;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.SharedSettings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.betonquest.reposilite.api.validation.Validatable;
import org.betonquest.reposilite.api.validation.ValidationLogLevel;
import org.betonquest.reposilite.api.validation.ValidationResult;
import org.betonquest.reposilite.api.validation.ValidationType;
import org.betonquest.reposilite.mapper.integration.VersionMapperFacade;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"MissingJavadoc", "FieldCanBeLocal", "PMD.CommentRequired", "PMD.FinalFieldCouldBeStatic", "PMD.DataClass", "PMD.MissingSerialVersionUID"})
@Doc(title = "VersionMapper", description = "All settings related to the advanced restful API")
public final class VersionMapperPluginSettings implements SharedSettings, Validatable<VersionMapperFacade> {

    @SuppressFBWarnings("SS_SHOULD_BE_STATIC")
    private final boolean namingConventionWarning = true;

    @SuppressFBWarnings("SS_SHOULD_BE_STATIC")
    private final boolean runExistenceChecks = true;

    private final ValidationLogLevel validationLogLevel = ValidationLogLevel.ALL;

    @SuppressFBWarnings("SE_BAD_FIELD")
    private final List<Artifact> artifacts = new ArrayList<>();

    public VersionMapperPluginSettings() {
    }

    @Override
    public List<ValidationResult> validate(final VersionMapperFacade facade) {
        final List<Artifact> artifacts = getArtifacts();
        final List<ValidationResult> results = new ArrayList<>();
        if (isNamingConventionWarning() && getValidationLogLevel() != ValidationLogLevel.IGNORE_ALL) {
            results.add(new ValidationResult("Running syntax tests...", ValidationType.INFO, List.of()));
            artifacts.stream().map(Artifact::validateNamingConvention).forEach(results::add);
        }
        if (isRunExistenceChecks() && getValidationLogLevel() != ValidationLogLevel.IGNORE_ALL) {
            results.add(new ValidationResult("Running semantics tests...", ValidationType.INFO, List.of()));
            artifacts.stream().map(artifact -> artifact.validateExistence(facade)).forEach(results::add);
        }
        return results;
    }

    @Doc(title = "Syntax Tests", description = "Prints warnings to console if naming conventions according to apache maven are violated.")
    public boolean isNamingConventionWarning() {
        return namingConventionWarning;
    }

    @Doc(title = "Semantics Tests", description = "Prints warnings to console if specified artifacts, repositories and such could not be found by their configuration.")
    public boolean isRunExistenceChecks() {
        return runExistenceChecks;
    }

    @Doc(title = "Log Level", description = "Change the amount of logs that are outputted.")
    public ValidationLogLevel getValidationLogLevel() {
        return validationLogLevel;
    }

    @Doc(title = "Artifacts", description = """
            All artifacts the are considered for listing requests.
            The id is supposed to be unique and is used for identifying the artifact in the advanced REST calls as well to enable swapping the artifact source easily.
            The repository simply defines a fixed default repository to source the artifact from. You can still address another repository via REST.
            The group id is the group id of the maven artifact. The artifact id is expect to be identical to how its defined in its configuration and without version.""")
    public List<Artifact> getArtifacts() {
        return artifacts;
    }
}
