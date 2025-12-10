package org.betonquest.reposilite.mapper.integration;

import com.reposilite.storage.api.Location;
import org.betonquest.reposilite.mapper.settings.Artifact;

/**
 * Represents a pom versioned entry in the maven repository.
 *
 * @param artifact     the versioned artifact
 * @param mavenVersion the maven version
 * @param pomVersion   the version defined in the pom and extracted from the artifact
 * @param jarLocation  the location of the jar file related to the pom
 */
public record PomVersionedEntry(Artifact artifact, String mavenVersion, String pomVersion, Location jarLocation) {

    /**
     * Checks if the maven version is a snapshot version.
     *
     * @return true if the maven version ends with "-SNAPSHOT", false otherwise
     */
    public boolean isSnapshot() {
        return mavenVersion.endsWith("-SNAPSHOT");
    }
}
