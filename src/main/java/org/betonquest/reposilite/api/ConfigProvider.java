package org.betonquest.reposilite.api;

import com.reposilite.configuration.shared.SharedConfigurationFacade;
import com.reposilite.configuration.shared.api.SharedSettings;
import com.reposilite.plugin.Extensions;
import panda.std.reactive.MutableReference;

/**
 * Provides a mutable reference to the plugin's configuration.
 *
 * @param <T> the type of the configuration
 */
@FunctionalInterface
public interface ConfigProvider<T extends SharedSettings> {

    /**
     * Creates a new {@link ConfigProvider} for the given {@link SharedSettings} class.
     *
     * @param clazz      the {@link SharedSettings} class
     * @param extensions the {@link Extensions} of the plugin
     * @param <T>        the type of the configuration
     * @return the {@link ConfigProvider} for the given {@link SharedSettings} class
     */
    static <T extends SharedSettings> ConfigProvider<T> forFacade(final Class<T> clazz, final Extensions extensions) {
        final SharedConfigurationFacade provider = extensions.facade(SharedConfigurationFacade.class);
        return () -> provider.getDomainSettings(clazz);
    }

    /**
     * Gets the mutable reference to the configuration.
     *
     * @return the mutable reference to the configuration
     */
    MutableReference<T> get();
}
