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

public class LimitedOptionalStringValidator extends OptionalStringValidator {
    private int maxLength;

    public LimitedOptionalStringValidator(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public ValidationResult validate(Object value) {
        final ValidationResult superResult = super.validate(value);

        if (superResult instanceof ValidationResult.ValidationPassed) {
            final String sValue = (String) value;
            if (sValue == null || sValue.length() <= maxLength) {
                new ValidationResult.ValidationPassed();
            } else {
                new ValidationResult.ValidationFailed("Value is longer than " + maxLength + " characters!");
            }
        }

        return superResult;
    }
}
