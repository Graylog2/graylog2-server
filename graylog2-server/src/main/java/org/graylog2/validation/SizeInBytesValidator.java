package org.graylog2.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class SizeInBytesValidator implements ConstraintValidator<SizeInBytes, String> {
    private int min;
    private int max;

    @Override
    public void initialize(final SizeInBytes annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(final String object,
                           final ConstraintValidatorContext constraintContext) {
        if (object == null) {
            return true;
        }

        final int lengthInBytes = object.getBytes(StandardCharsets.UTF_8).length;
        return lengthInBytes >= this.min && lengthInBytes <= this.max;
    }
}
