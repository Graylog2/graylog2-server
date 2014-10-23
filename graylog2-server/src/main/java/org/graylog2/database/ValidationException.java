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
package org.graylog2.database;

import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, List<ValidationResult>> errors;

    public ValidationException(Map<String, List<ValidationResult>> errors) {
        this.errors = errors;
    }

    public ValidationException(final String message) {
        this("_", message);
    }

    public ValidationException(final String field, final String message) {
        this.errors = new HashMap<>();
        this.errors.put(field, Collections.<ValidationResult>singletonList(new ValidationResult.ValidationFailed(message)));
    }

    public Map<String, List<ValidationResult>> getErrors() {
        return errors;
    }
}
