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

import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateValidator implements Validator {

    /**
     * Validates: Object is not null, of type org.joda.time.DateTime and
     * the String representation is in UTC.
     *
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public ValidationResult validate(Object value) {
        if (value != null && (value instanceof org.joda.time.DateTime) && value.toString().endsWith("Z"))
            return new ValidationResult.ValidationPassed();
        else
            return new ValidationResult.ValidationFailed(value + " is not a valid date!");
    }

}
