package org.betonquest.reposilite.api.validation;

import java.util.List;

/**
 * Interface for validatable classes.
 *
 * @param <T> The type of the requirement for validation
 */
@FunctionalInterface
public interface Validatable<T> {

    /**
     * Validates using the given requirement.
     *
     * @param requirement The requirement for validation.
     * @return A list of validation results.
     */
    List<ValidationResult> validate(T requirement);
}
