package org.betonquest.reposilite.mapper.integration;

import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.Repository;
import com.reposilite.maven.api.LookupRequest;
import com.reposilite.maven.api.ResolvedDocument;
import com.reposilite.maven.api.VersionLookupRequest;
import com.reposilite.maven.api.VersionsResponse;
import com.reposilite.plugin.api.EventListener;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.shared.ErrorResponse;
import com.reposilite.storage.api.FileDetails;
import com.reposilite.storage.api.Location;
import org.betonquest.reposilite.api.PluginAdapter;
import org.betonquest.reposilite.mapper.settings.Artifact;
import org.betonquest.reposilite.mapper.settings.VersionMapperPluginSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import panda.std.Result;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base {@link Facade} for the VersionMapperPlugin.
 */
public class VersionMapperFacade implements Facade, EventListener<ReposiliteInitializeEvent> {

    /**
     * The {@link PluginAdapter} of the VersionMapperPlugin.
     */
    private final PluginAdapter<VersionMapperFacade, VersionMapperPluginSettings> plugin;

    /**
     * The {@link XPathFactory} to create {@link XPath} instances.
     */
    private final XPathFactory xPathFactory;

    /**
     * The {@link DocumentBuilderFactory} to create {@link DocumentBuilder} instances.
     */
    private final DocumentBuilderFactory documentBuilderFactory;

    /**
     * The {@link ArtifactsVersionsCache} to access cached artifact versions.
     */
    private final ArtifactsVersionsCache artifactsVersionsCache;

    /**
     * The {@link MavenFacade} to access maven repositories.
     */
    private MavenFacade mavenFacade;

    /**
     * The constructor of the BaseFacade.
     *
     * @param plugin                 The {@link PluginAdapter} of the VersionMapperPlugin.
     * @param artifactsVersionsCache The {@link ArtifactsVersionsCache} to access cached artifact versions.
     */
    public VersionMapperFacade(final PluginAdapter<VersionMapperFacade, VersionMapperPluginSettings> plugin, final ArtifactsVersionsCache artifactsVersionsCache) {
        this.plugin = plugin;
        this.xPathFactory = XPathFactory.newInstance();
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.artifactsVersionsCache = artifactsVersionsCache;
    }

    private XPath getXPath() {
        return xPathFactory.newXPath();
    }

    /**
     * Returns the {@link PluginAdapter} of the VersionMapperPlugin.
     *
     * @return The {@link PluginAdapter} of the VersionMapperPlugin.
     */
    public PluginAdapter<VersionMapperFacade, VersionMapperPluginSettings> getPlugin() {
        return plugin;
    }

    /**
     * Returns a new {@link DocumentBuilder} instance.
     *
     * @return The new {@link DocumentBuilder} instance.
     * @throws ParserConfigurationException If the {@link DocumentBuilderFactory} is not configured correctly.
     */
    public DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return documentBuilderFactory.newDocumentBuilder();
    }

    /**
     * Returns the {@link ArtifactsVersionsCache} to access cached artifact versions.
     *
     * @return The {@link ArtifactsVersionsCache} to access cached artifact versions.
     */
    public ArtifactsVersionsCache getArtifactsVersionsCache() {
        return artifactsVersionsCache;
    }

    @Override
    public void onCall(@NotNull final ReposiliteInitializeEvent reposiliteInitializeEvent) {
        mavenFacade = plugin.getFacade(MavenFacade.class);
    }

    /**
     * Checks if the given repository is known by the {@link MavenFacade}.
     *
     * @param repository The repository to check.
     * @return True if the repository is known, false otherwise.
     */
    public boolean isRepositoryKnown(final String repository) {
        return repository != null && mavenFacade.getRepository(repository) != null;
    }

    /**
     * Checks if an artifact exists in the given repository at the given location.
     *
     * @param repository The repository to check.
     * @param location   The location of the artifact.
     * @return True if the artifact exists, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasArtifact(final String repository, final Location location) {
        if (repository == null || location == null) {
            return false;
        }
        final Result<? extends FileDetails, ErrorResponse> result = mavenFacade.findDetails(new LookupRequest(null, repository, location));
        return result.isOk();
    }

    /**
     * Finds an artifact in the given repository with the given gav.
     *
     * @param repository The repository to search in.
     * @param gav        The gav to search for.
     * @return The matching artifact if found, null otherwise.
     */
    @Nullable
    public Artifact findArtifact(final String repository, final Location gav) {
        if (!hasArtifact(repository, gav)) {
            return null;
        }
        final VersionMapperPluginSettings pluginSettings = this.plugin.getConfig().get();
        final List<Artifact> artifacts = pluginSettings.getArtifacts();
        return artifacts.stream().filter(artifact ->
                artifact.repository().equals(repository) && artifact.gav().equals(gav)).findAny().orElse(null);
    }

    /**
     * Read all versions known to {@link MavenFacade} for a given artifact.
     * Maps all versions according to the configured XPath expression in the artifact
     * settings to create {@link PomVersionedEntry}s.
     *
     * @param artifact The artifact to map versions for.
     * @return a list of {@link PomVersionedEntry} containing all known versions of the artifact
     * or an empty list if the artifact does not exist.
     * @throws XPathExpressionException     if the configured XPath expression is invalid.
     * @throws ParserConfigurationException if the {@link DocumentBuilderFactory} is not configured correctly.
     */
    protected List<PomVersionedEntry> getMappedVersions(final Artifact artifact) throws XPathExpressionException, ParserConfigurationException {
        final Location gav = artifact.gav();
        final Repository repository = mavenFacade.getRepository(artifact.repository());
        if (repository == null || !hasArtifact(artifact.repository(), gav)) {
            return List.of();
        }

        final XPathExpression xpathExpression = getXPath().compile(artifact.versionXPath());
        final DocumentBuilder documentBuilder = getDocumentBuilder();

        final List<PomVersionedEntry> versions = new ArrayList<>();
        final List<String> mavenVersions = getMavenVersions(artifact.repository(), gav);
        for (final String version : mavenVersions) {
            final Location pom = artifact.versionedGav(version, ".pom");
            final Result<ResolvedDocument, ErrorResponse> pomFile = mavenFacade.findFile(new LookupRequest(null, artifact.repository(), pom));
            if (pomFile.isErr()) {
                plugin.warn(pomFile.getError().getMessage());
                continue;
            }
            try {
                final Document parse = documentBuilder.parse(pomFile.get().getContent());
                final String pomVersion = (String) xpathExpression.evaluate(parse, XPathConstants.STRING);
                final Location jar = artifact.versionedGav(version, ".jar");
                versions.add(new PomVersionedEntry(artifact, version, pomVersion, jar));
            } catch (SAXException | IOException | IllegalStateException exception) {
                plugin.warn("Error while generating version mappings. " + exception.getMessage());
                plugin.getLogger().exception(exception);
            }
        }
        return versions;
    }

    private List<String> getMavenVersions(final String repository, final Location location) {
        final Repository repo = mavenFacade.getRepository(repository);
        if (repo == null) {
            return List.of();
        }
        final Result<VersionsResponse, ErrorResponse> versions = mavenFacade.findVersions(new VersionLookupRequest(null, repo, location, null));
        if (versions.isErr()) {
            return List.of();
        }
        return versions.get().getVersions();
    }
}
