package org.graylog.events.fields.validators;

import java.util.List;
import java.util.Optional;

public interface FieldTypeValidator {
    /**
     * Validates the given value for compliance with a data type.
     *
     * @param value the value to validate
     * @return optionally list of validation error messages
     */
    Optional<List<String>> validate(String value);
}
