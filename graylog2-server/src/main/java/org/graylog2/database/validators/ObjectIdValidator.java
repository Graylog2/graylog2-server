/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.database.validators;

import org.bson.types.ObjectId;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;

import javax.annotation.Nullable;

public class ObjectIdValidator implements Validator {
    /**
     * Validates: Object is not {@code null} and of type {@link ObjectId}.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public ValidationResult validate(@Nullable final Object value) {
        if (value instanceof ObjectId) {
            return new ValidationResult.ValidationPassed();
        } else {
            return new ValidationResult.ValidationFailed(String.valueOf(value) + " is not a valid ObjectId!");
        }
    }
}
