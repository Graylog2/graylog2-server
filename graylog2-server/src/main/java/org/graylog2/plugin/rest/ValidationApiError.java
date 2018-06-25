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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.List;
import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class ValidationApiError implements GenericError {
    @JsonProperty
    public abstract Map<String, List<ValidationResult>> validationErrors();

    public static ValidationApiError create(String message, Map<String, List<ValidationResult>> validationErrors) {
        return new AutoValue_ValidationApiError(message, ImmutableMap.copyOf(validationErrors));
    }
}
