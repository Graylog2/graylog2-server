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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

@JsonAutoDetect
public class ValidationResult {

    private final Multimap<String, String> errors = ArrayListMultimap.create();
    private final Multimap<String, String> context = ArrayListMultimap.create();


    public void addError(String fieldName, String error) {
        errors.put(fieldName, error);
    }
    public void addContext(String fieldName, Iterable<String> values) {
        context.putAll(fieldName, values);
    }

    public void addAll(Multimap<String, String> extraErrors) {
        errors.putAll(extraErrors);
    }

    public void addAll(ValidationResult validationResult) {
        errors.putAll(validationResult.errors);
    }

    @JsonProperty("failed")
    public boolean failed() {
        return !errors.isEmpty();
    }

    @JsonProperty("errors")
    public Map<String, Collection<String>> getErrors() {
        return errors.asMap();
    }

    @JsonProperty("error_context")
    public Map<String, Collection<String>> getContext() {
        return context.asMap();
    }
}
