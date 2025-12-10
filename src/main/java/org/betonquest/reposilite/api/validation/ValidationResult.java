package org.betonquest.reposilite.api.validation;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a validation result.
 *
 * @param message The message of the result.
 * @param result  The result type.
 * @param details Additional details.
 */
public record ValidationResult(String message, ValidationType result, @Unmodifiable List<String> details) {

    /**
     * Prints all validation results combined into specified consumers filtered by log level.
     *
     * @param results  The validation results to print.
     * @param warn     The warning printer.
     * @param info     The info printer.
     * @param logLevel The log level to print.
     */
    public static void printBlock(final List<ValidationResult> results, final Consumer<String> warn, final Consumer<String> info, final ValidationLogLevel logLevel) {
        results.forEach(result -> result.print(warn, info, logLevel));
    }

    /**
     * Prints the validation result to the specified consumers filtered by log level.
     *
     * @param warn     The warning printer.
     * @param info     The info printer.
     * @param logLevel The log level to print.
     */
    public void print(final Consumer<String> warn, final Consumer<String> info, final ValidationLogLevel logLevel) {
        if (ValidationType.forLevel(logLevel).getPriority() < result.getPriority()) {
            return;
        }
        final Consumer<String> printer = result == ValidationType.ERROR ? warn : info;
        printer.accept(message);
        details.forEach(detail -> printer.accept(" > " + detail));
    }
}
