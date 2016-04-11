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
package org.graylog2.plugin.database;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, List<ValidationResult>> errors;

    public ValidationException(Map<String, List<ValidationResult>> errors) {
        this.errors = ImmutableMap.copyOf(errors);
    }

    public ValidationException(final String message) {
        this("_", message);
    }

    public ValidationException(final String field, final String message) {
        super(message);
        this.errors = ImmutableMap.of(field, Collections.<ValidationResult>singletonList(new ValidationResult.ValidationFailed(message)));
    }

    public Map<String, List<ValidationResult>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("message", getLocalizedMessage())
            .add("errors", errors)
            .toString();
    }
}
