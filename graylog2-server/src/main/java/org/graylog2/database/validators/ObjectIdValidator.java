package org.graylog2.database.validators;

import org.bson.types.ObjectId;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ObjectIdValidator implements Validator{
    /**
     * Validates: Object is not null and of type ObjectId.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public boolean validate(Object value) {
        return value != null && value instanceof ObjectId;
    }
}
