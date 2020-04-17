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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalInt;

import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;

@AutoValue
@JsonDeserialize(builder = ResultFormat.Builder.class)
public abstract class ResultFormat {
    @NotEmpty
    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract LinkedHashSet<Sort> sort();

    @Positive
    public abstract OptionalInt limit();

    public abstract Map<String, Object> executionState();

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
            return fieldsInOrder(linkedHashSetOf(fields));
        }

        @JsonProperty
        public abstract Builder sort(LinkedHashSet<Sort> sort);

        public Builder sort(Sort... sorts) {
            return sort(linkedHashSetOf(sorts));
        }

        @JsonProperty
        public abstract Builder limit(Integer limit);

        @JsonProperty
        public abstract Builder executionState(Map<String, Object> executionState);

        abstract ResultFormat autoBuild();

        public ResultFormat build() {
            return autoBuild();
        }

        @JsonCreator
        public static ResultFormat.Builder create() {
            return new AutoValue_ResultFormat.Builder()
                    .fieldsInOrder(MessagesRequest.DEFAULT_FIELDS)
                    .executionState(Collections.emptyMap())
                    .sort();
        }
    }
}
