package org.betonquest.reposilite.mapper.command;

import com.reposilite.console.CommandContext;
import com.reposilite.console.api.ReposiliteCommand;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

/**
 * Reposilite cli command to update the cache of the version mapper plugin.
 */
@CommandLine.Command(
        name = "update-cache",
        description = "Updates the cache of the version mapper plugin."
)
public class UpdateCacheCommand implements ReposiliteCommand {

    /**
     * The runnable to execute to update the cache.
     */
    private final Runnable updateCacheRunnable;

    /**
     * Constructor for the UpdateCacheCommand.
     *
     * @param updateCacheRunnable The runnable to execute to update the cache.
     */
    public UpdateCacheCommand(final Runnable updateCacheRunnable) {
        this.updateCacheRunnable = updateCacheRunnable;
    }

    @Override
    public void execute(@NotNull final CommandContext commandContext) {
        this.updateCacheRunnable.run();
    }
}
