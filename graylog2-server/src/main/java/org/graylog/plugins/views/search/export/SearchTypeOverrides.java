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
package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.Sort;

import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = SearchTypeOverrides.Builder.class)
public abstract class SearchTypeOverrides {
    public abstract Optional<Set<String>> fieldsInOrder();

    public abstract Optional<Set<Sort>> sort();

    public static SearchTypeOverrides.Builder builder() {
        return SearchTypeOverrides.Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("fields_in_order")
        public abstract Builder fieldsInOrder(Set<String> fieldsInOrder);

        @JsonProperty
        public abstract Builder sort(Set<Sort> sort);

        abstract SearchTypeOverrides autoBuild();

        public SearchTypeOverrides build() {
            return autoBuild();
        }

        @JsonCreator
        public static SearchTypeOverrides.Builder create() {
            return new AutoValue_SearchTypeOverrides.Builder();
        }
    }
}
