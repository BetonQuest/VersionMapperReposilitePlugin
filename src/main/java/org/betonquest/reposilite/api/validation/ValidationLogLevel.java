package org.betonquest.reposilite.api.validation;

/**
 * The log level for validation results.
 */
public enum ValidationLogLevel {

    /**
     * Prints all validation results.
     */
    ALL,
    /**
     * Prints only validation results with errors.
     */
    INFO,
    /**
     * Prints only validation results with errors.
     */
    ERRORS_ONLY,
    /**
     * Ignores all validation results.
     */
    IGNORE_ALL
}
