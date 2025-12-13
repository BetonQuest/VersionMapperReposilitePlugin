package org.betonquest.reposilite.mapper;

import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.plugin.api.ReposilitePostInitializeEvent;
import com.reposilite.plugin.api.ReposiliteStartedEvent;
import com.reposilite.web.api.ReposiliteRoute;
import com.reposilite.web.api.RoutingSetupEvent;
import org.betonquest.reposilite.api.PluginAdapter;
import org.betonquest.reposilite.api.validation.ValidationLogLevel;
import org.betonquest.reposilite.api.validation.ValidationResult;
import org.betonquest.reposilite.mapper.integration.ArtifactsVersionsCache;
import org.betonquest.reposilite.mapper.integration.VersionMapperFacade;
import org.betonquest.reposilite.mapper.restful.RestfulRoutes;
import org.betonquest.reposilite.mapper.settings.Artifact;
import org.betonquest.reposilite.mapper.settings.VersionMapperPluginSettings;
import panda.std.reactive.MutableReference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The VersionMapper main class for Reposilite.
 */
@Plugin(name = "VersionMapper", settings = VersionMapperPluginSettings.class, dependencies = {"shared-configuration", "maven"})
public class VersionMapperPlugin extends PluginAdapter<VersionMapperFacade, VersionMapperPluginSettings> {

    /**
     * The cache of all {@link Artifact} versions as defined in the plugin settings.
     */
    private final ArtifactsVersionsCache artifactsVersionsCache;

    /**
     * The facade for the plugin as api via reposilite.
     */
    private final VersionMapperFacade baseFacade;

    /**
     * The implementation of the restful endpoints.
     */
    private RestfulRoutes restfulImplementation;

    /**
     * Default Constructor for the VersionMapperPlugin.
     */
    public VersionMapperPlugin() {
        super("VersionMapper", VersionMapperFacade.class, VersionMapperPluginSettings.class);
        this.artifactsVersionsCache = new ArtifactsVersionsCache(this);
        this.baseFacade = new VersionMapperFacade(this, this.artifactsVersionsCache);
    }

    @Override
    public Facade onLoad() {
        extensions().registerEvent(ReposiliteInitializeEvent.class, this.baseFacade);
        return baseFacade;
    }

    @Override
    public void onInitialize(final ReposiliteInitializeEvent event) {
        info("Initialized!");

        this.restfulImplementation = new RestfulRoutes(getFacade(MavenFacade.class), baseFacade);

        final MutableReference<VersionMapperPluginSettings> config = getConfig();
        final VersionMapperPluginSettings settings = config.get();
        config.subscribe(sets -> ValidationResult.printBlock(sets.validate(baseFacade), this::warn, this::info, ValidationLogLevel.ERRORS_ONLY));

        final List<String> artifacts = settings.getArtifacts().stream().map(Artifact::id).toList();
        final String list = String.join(", ", artifacts);
        info("Loaded " + artifacts.size() + " artifacts: ");
        info("  > " + list);
    }

    @Override
    public void onEnable(final ReposilitePostInitializeEvent event) {
        info("Attempting to generate cache...");
        updateCache();
        info("Cache generation complete.");
    }

    @Override
    public void onStart(final ReposiliteStartedEvent event) {
        final List<ValidationResult> validate = getConfig().get().validate(baseFacade);
        ValidationResult.printBlock(validate, this::warn, this::info, ValidationLogLevel.ALL);
        getConfig().subscribe(settings -> updateCache());
    }

    @Override
    public void onDeploy(final DeployEvent event) {
        final Artifact artifact = baseFacade.findArtifact(event.getRepository().getName(), event.getGav());
        if (artifact == null) {
            return;
        }
        if (artifactsVersionsCache.hasEntry(artifact.artifactId())) {
            updateCache();
        }
    }

    @Override
    public void onRoutingSetup(final RoutingSetupEvent event) {
        event.registerRoutes(this.restfulImplementation);
        info("Mapper routes registered: " + this.restfulImplementation.getRoutes().stream().map(ReposiliteRoute::getPath).collect(Collectors.joining(", ")));
    }

    private void updateCache() {
        final List<Artifact> artifacts = getConfig().get().getArtifacts();
        for (final Artifact artifact : artifacts) {
            if (artifactsVersionsCache.attemptToCache(artifact)) {
                info("  > \"" + artifact.id() + "\" cache generated. (" + artifactsVersionsCache.getVersionsCount(artifact.id()) + " versions)");
            } else {
                warn("  > \"" + artifact.id() + "\" cache generation failed");
            }
        }
    }
}
