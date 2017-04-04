package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.util.List;

public class NonEmptyListValidator implements Validator<List> {
    @Override
    public void validate(String name, List value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Parameter " + name + " should be non-empty list (found " + value + ")");
        }
    }
}
