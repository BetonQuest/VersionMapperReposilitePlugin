package org.betonquest.reposilite.mapper.integration;

import com.reposilite.storage.api.Location;
import org.betonquest.reposilite.mapper.settings.Artifact;

/**
 * Represents a pom versioned entry in the maven repository.
 *
 * @param group       the group of the artifact
 * @param artifact    the versioned artifact
 * @param maven       the maven version
 * @param pom         the version defined in the pom and extracted from the artifact
 * @param jarLocation the location of the jar file related to the pom
 */
public record PomVersionedEntry(Artifact artifact, String group, String maven, String pom, Location jarLocation) {

    /**
     * Checks if the maven version is a snapshot version.
     *
     * @return true if the maven version ends with "-SNAPSHOT", false otherwise
     */
    public boolean isSnapshot() {
        return group.endsWith("-SNAPSHOT");
    }
}
