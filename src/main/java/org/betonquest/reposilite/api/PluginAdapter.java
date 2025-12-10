package org.betonquest.reposilite.api;

import com.reposilite.Reposilite;
import com.reposilite.configuration.shared.api.SharedSettings;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.ReposiliteDisposeEvent;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.plugin.api.ReposilitePlugin;
import com.reposilite.plugin.api.ReposilitePostInitializeEvent;
import com.reposilite.plugin.api.ReposiliteStartedEvent;
import com.reposilite.web.api.RoutingSetupEvent;
import org.jetbrains.annotations.Nullable;
import panda.std.reactive.MutableReference;

/**
 * Custom API on top of reposilite's plugin api. Base class for Reposilite plugins.
 *
 * @param <F> the {@link Facade} api type of the plugin
 * @param <T> the {@link SharedSettings} type of the plugin
 */
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class PluginAdapter<F extends Facade, T extends SharedSettings> extends ReposilitePlugin {

    /**
     * The defined {@link SharedSettings} class of the plugin.
     */
    private final Class<T> settingsClass;

    /**
     * The defined {@link Facade} class of the plugin.
     */
    private final Class<F> facadeClass;

    /**
     * The prefix for all log messages of the plugin directed through this {@link PluginAdapter}.
     */
    private final String loggerPrefix;

    /**
     * The {@link ConfigProvider} for the {@link SharedSettings} of the plugin.
     */
    private ConfigProvider<T> configProvider;

    /**
     * Default Constructor for the PluginAdapter.
     *
     * @param pluginName    the name of the plugin
     * @param facadeClass   the {@link Facade} of the plugin
     * @param settingsClass the {@link SharedSettings} of the plugin
     */
    public PluginAdapter(final String pluginName, final Class<F> facadeClass, final Class<T> settingsClass) {
        super();
        this.settingsClass = settingsClass;
        this.facadeClass = facadeClass;
        this.loggerPrefix = "[" + pluginName + "] ";
    }

    @Override
    public @Nullable Facade initialize() {
        final Facade facade = onLoad();

        extensions().registerEvent(ReposiliteInitializeEvent.class, this::onInitialize);
        extensions().registerEvent(ReposilitePostInitializeEvent.class, this::onEnable);
        extensions().registerEvent(ReposiliteStartedEvent.class, this::onStart);
        extensions().registerEvent(ReposiliteDisposeEvent.class, this::onDispose);
        extensions().registerEvent(RoutingSetupEvent.class, this::onRoutingSetup);
        extensions().registerEvent(DeployEvent.class, this::onDeploy);

        return facade;
    }

    /**
     * Called first when the plugin is loaded.
     * Initialize ordering: 1
     *
     * @return the facade of the plugin
     */
    public Facade onLoad() {
        return null;
    }

    /**
     * Called after directly the plugin is loaded. Defined dependencies are available.
     * Initialize ordering: 2
     *
     * @param event the {@link ReposiliteInitializeEvent} called for this plugin
     */
    public void onInitialize(final ReposiliteInitializeEvent event) {
        // Empty
    }

    /**
     * Called after the plugin is enabled.
     * Initialize ordering: 3
     *
     * @param event the {@link ReposilitePostInitializeEvent} called for this plugin
     */
    public void onEnable(final ReposilitePostInitializeEvent event) {
        // Empty
    }

    /**
     * Called after the plugin is started.
     *
     * @param event the {@link ReposiliteStartedEvent} called for this plugin
     */
    public void onStart(final ReposiliteStartedEvent event) {
        // Empty
    }

    /**
     * Called when the plugin is disposed of.
     *
     * @param event the {@link ReposiliteDisposeEvent} called for this plugin
     */
    public void onDispose(final ReposiliteDisposeEvent event) {
        // Empty
    }

    /**
     * Called when a file is deployed.
     *
     * @param event the {@link DeployEvent} called for this plugin
     */
    public void onDeploy(final DeployEvent event) {
        // Empy
    }

    /**
     * Called when the routing is set up.
     *
     * @param event the {@link RoutingSetupEvent} called for this plugin
     */
    public void onRoutingSetup(final RoutingSetupEvent event) {
        // Empty
    }

    /**
     * Access to the {@link Reposilite} facade instance.
     * Calls {@link ReposilitePlugin#extensions()} internally.
     *
     * @return the {@link Reposilite} facade instance
     */
    public Reposilite getReposilite() {
        return extensions().facade(Reposilite.class);
    }

    /**
     * Access to the {@link F} facade instance.
     * Calls {@link ReposilitePlugin#extensions()} internally.
     *
     * @return the {@link F} facade instance
     */
    public F getPluginFacade() {
        return extensions().facade(facadeClass);
    }

    /**
     * Access to the {@link SharedSettings} of the plugin.
     *
     * @return the {@link SharedSettings} of the plugin
     */
    public MutableReference<T> getConfig() {
        if (configProvider == null) {
            configProvider = ConfigProvider.forFacade(settingsClass, extensions());
        }
        return configProvider.get();
    }

    /**
     * Access to any {@link Facade} via {@link ReposilitePlugin#extensions()}.
     *
     * @param clazz the {@link Facade} class
     * @param <G>   the {@link Facade} type
     * @return the {@link Facade} instance
     */
    public <G extends Facade> G getFacade(final Class<G> clazz) {
        return extensions().facade(clazz);
    }

    /**
     * The prefix for all log messages of the plugin directed through this {@link PluginAdapter}.
     *
     * @return the logger prefix
     */
    public String getLoggerPrefix() {
        return loggerPrefix;
    }

    /**
     * Logs a warning message with the plugin prefix.
     *
     * @param message the warning to log
     * @see #getLoggerPrefix()
     */
    public void warn(final String message) {
        extensions().getLogger().warn(getLoggerPrefix() + message);
    }

    /**
     * Logs an info message with the plugin prefix.
     *
     * @param message the message to log
     * @see #getLoggerPrefix()
     */
    public void info(final String message) {
        extensions().getLogger().info(getLoggerPrefix() + message);
    }
}
