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
package org.graylog2.migrations.V20180214093600_AdjustDashboardPositionToNewResolution;

import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;

class OptionalStringValidator implements Validator {
    /**
     * Validates: Object is null or of type String.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public ValidationResult validate(Object value) {
        if (value == null || value instanceof String) {
            return new ValidationResult.ValidationPassed();
        } else {
            return new ValidationResult.ValidationFailed("Value \"" + value + "\" is not a valid string!");
        }
    }
}
