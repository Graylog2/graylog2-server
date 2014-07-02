package org.graylog2.database.validators;

public class LimitedStringValidator extends FilledStringValidator {
    private int minLength;
    private int maxLength;

    public LimitedStringValidator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Validates: applies the validation from FilledStringValidator and also check that value's length
     * is between the minimum and maximum length passed to the constructor.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public boolean validate(Object value) {
        if (super.validate(value)) {
            String sValue = (String)value;
            return sValue.length() >= minLength && sValue.length() <= maxLength;
        }
        return false;
    }
}
