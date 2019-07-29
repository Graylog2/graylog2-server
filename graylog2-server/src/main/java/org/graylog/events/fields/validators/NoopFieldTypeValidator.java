package org.graylog.events.fields.validators;


import java.util.List;
import java.util.Optional;

public class NoopFieldTypeValidator implements FieldTypeValidator {
    public static final FieldTypeValidator INSTANCE = new NoopFieldTypeValidator();

    @Override
    public Optional<List<String>> validate(String value) {
        return Optional.empty();
    }
}
