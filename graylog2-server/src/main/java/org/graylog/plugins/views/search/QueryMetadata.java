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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
public abstract class QueryMetadata {

    @JsonProperty("used_parameters_names")
    public abstract ImmutableSet<String> usedParameterNames();

    @JsonProperty("referenced_queries")
    public abstract ImmutableSet<String> referencedQueries();

    public static QueryMetadata empty() {
        return QueryMetadata.builder().build();
    }

    public static Builder builder() {
        return new AutoValue_QueryMetadata.Builder()
                .usedParameterNames(of())
                .referencedQueries(of());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("used_parameters_names")
        public abstract Builder usedParameterNames(Set<String> usedParameterNames);

        @JsonProperty("referenced_queries")
        public abstract Builder referencedQueries(Set<String> referencedQueries);

        public abstract QueryMetadata build();
    }
}
