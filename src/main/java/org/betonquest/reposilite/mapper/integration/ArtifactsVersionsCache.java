package org.betonquest.reposilite.mapper.integration;

import org.betonquest.reposilite.api.PluginAdapter;
import org.betonquest.reposilite.mapper.settings.Artifact;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ArtifactsVersionsCache for the VersionMapperPlugin.
 */
public class ArtifactsVersionsCache {

    /**
     * The {@link PluginAdapter} of the VersionMapperPlugin.
     */
    private final PluginAdapter<VersionMapperFacade, ?> plugin;

    /**
     * The cache of all {@link Artifact} versions as defined in the plugin settings.
     */
    private final Map<String, List<PomVersionedEntry>> pomVersionedEntryCache;

    /**
     * The constructor for the ArtifactsVersionsCache.
     *
     * @param plugin The {@link PluginAdapter} of the VersionMapperPlugin.
     */
    public ArtifactsVersionsCache(final PluginAdapter<VersionMapperFacade, ?> plugin) {
        this.pomVersionedEntryCache = new HashMap<>();
        this.plugin = plugin;
    }

    /**
     * Checks if the cache contains an entry for the given artifact config id.
     *
     * @param artifactConfigId The artifact config id to check.
     * @return True if the cache contains an entry for the given artifact config id, false otherwise.
     */
    public boolean hasEntry(final String artifactConfigId) {
        return pomVersionedEntryCache.containsKey(artifactConfigId);
    }

    /**
     * Returns the number of versions for the given artifact config id.
     *
     * @param artifactConfigId The artifact config id to check.
     * @return The number of versions for the given artifact config id.
     */
    public int getVersionsCount(final String artifactConfigId) {
        return getVersions(artifactConfigId).size();
    }

    /**
     * Returns all versions for the given artifact config id.
     *
     * @param artifactConfigId The artifact config id to check.
     * @return All versions for the given artifact config id.
     */
    public List<PomVersionedEntry> getVersions(final String artifactConfigId) {
        return pomVersionedEntryCache.getOrDefault(artifactConfigId, Collections.emptyList());
    }

    /**
     * Tries to cache the versions for the given artifact.
     *
     * @param artifact The artifact to cache.
     * @return True if the artifact was successfully cached, false otherwise.
     */
    public boolean attemptToCache(final Artifact artifact) {
        final VersionMapperFacade baseFacade = plugin.getPluginFacade();
        try {
            final List<PomVersionedEntry> mappedVersions = baseFacade.getMappedVersions(artifact);
            if (mappedVersions.isEmpty()) {
                return false;
            }
            pomVersionedEntryCache.put(artifact.id(), mappedVersions);
        } catch (XPathExpressionException | ParserConfigurationException e) {
            baseFacade.getPlugin().getLogger().exception(e);
            return false;
        }
        return true;
    }
}
