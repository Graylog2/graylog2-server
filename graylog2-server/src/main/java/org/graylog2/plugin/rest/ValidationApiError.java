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
package org.graylog2.plugin.rest;

import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ValidationApiError extends ApiError {
    private final Map<String, List<ValidationResult>> validationErrors;
    public ValidationApiError(String message, Map<String, List<ValidationResult>> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public Map<String, List<ValidationResult>> getValidationErrors() {
        return validationErrors;
    }
}
