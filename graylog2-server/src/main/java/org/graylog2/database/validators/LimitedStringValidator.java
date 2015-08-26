/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.database.validators;

import org.graylog2.plugin.database.validators.ValidationResult;

import static com.google.common.base.Preconditions.checkArgument;

public class LimitedStringValidator extends FilledStringValidator {
    private final int minLength;
    private final int maxLength;

    public LimitedStringValidator(int minLength, int maxLength) {
        checkArgument(minLength > 0, "minLength must be greater than 0");
        checkArgument(maxLength > 0, "maxLength must be greater than 0");
        checkArgument(minLength <= maxLength, "maxLength must be greater than or equal to minLength");

        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Validates: applies the validation from {@link FilledStringValidator} and also check that value's length
     * is between the minimum and maximum length passed to the constructor.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public ValidationResult validate(Object value) {
        ValidationResult result = super.validate(value);
        if (result instanceof ValidationResult.ValidationPassed) {
            final String sValue = (String)value;
            if (sValue.length() < minLength || sValue.length() > maxLength) {
                result = new ValidationResult.ValidationFailed("Value is not between " + minLength + " and " + maxLength + " in length!");
            }
        }
        return result;
    }
}
