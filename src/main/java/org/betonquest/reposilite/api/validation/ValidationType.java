package org.betonquest.reposilite.api.validation;

/**
 * The validation type for a validation result.
 */
public enum ValidationType {
    /**
     * The validation was successful.
     */
    SUCCESS(3),
    /**
     * The validation was successful, but with additional information.
     */
    INFO(2),
    /**
     * The validation failed with an error.
     */
    ERROR(1),
    /**
     * No validation was performed.
     */
    NONE(0);

    /**
     * The priority of the validation type for ordering and filtering.
     */
    private final int priority;

    ValidationType(final int priority) {
        this.priority = priority;
    }

    /**
     * Returns the validation type for the given log level.
     *
     * @param logLevel The log level to convert.
     * @return The validation type for the given log level.
     */
    public static ValidationType forLevel(final ValidationLogLevel logLevel) {
        return switch (logLevel) {
            case IGNORE_ALL -> NONE;
            case ERRORS_ONLY -> ERROR;
            case INFO -> INFO;
            case ALL -> SUCCESS;
        };
    }

    /**
     * Returns the priority of the validation type.
     *
     * @return the priority of the validation type
     */
    public int getPriority() {
        return priority;
    }
}
