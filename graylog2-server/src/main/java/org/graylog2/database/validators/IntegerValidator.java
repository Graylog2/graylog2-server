package org.graylog2.database.validators;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class IntegerValidator implements Validator {
    /**
     * Validates: Object is not null and of type Integer.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public boolean validate(Object value) {
        return value != null && value instanceof Integer;
    }
}
