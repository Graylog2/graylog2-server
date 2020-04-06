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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = ResultFormat.Builder.class)
public abstract class ResultFormat {
    public abstract Optional<LinkedHashSet<String>> fieldsInOrder();

    public abstract Optional<LinkedHashSet<Sort>> sort();

    public static ResultFormat.Builder builder() {
        return ResultFormat.Builder.create();
    }

    public static ResultFormat empty() {
        return ResultFormat.builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("fields_in_order")
        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public Builder fieldsInOrder(String... fields) {
            LinkedHashSet<String> fieldsSet = Arrays.stream(fields).collect(Collectors.toCollection(LinkedHashSet::new));
            return fieldsInOrder(fieldsSet);
        }

        @JsonProperty
        public abstract Builder sort(LinkedHashSet<Sort> sort);

        public Builder sort(Sort... sorts) {
            LinkedHashSet<Sort> sortsSet = Arrays.stream(sorts).collect(Collectors.toCollection(LinkedHashSet::new));
            return sort(sortsSet);
        }

        abstract ResultFormat autoBuild();

        public ResultFormat build() {
            return autoBuild();
        }

        @JsonCreator
        public static ResultFormat.Builder create() {
            return new AutoValue_ResultFormat.Builder();
        }
    }
}
