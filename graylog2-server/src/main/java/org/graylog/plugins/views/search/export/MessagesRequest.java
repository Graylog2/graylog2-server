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
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = MessagesRequest.Builder.class)
public abstract class MessagesRequest {
    public abstract Optional<TimeRange> timeRange();

    public abstract Optional<BackendQuery> queryString();

    public abstract Optional<Set<String>> streams();

    public abstract Optional<Set<String>> fieldsInOrder();

    public abstract Optional<Set<Sort>> sort();

    public static MessagesRequest empty() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public void ensureCompleteness() {
        List<String> missingFields = new ArrayList<>();
        if (!timeRange().isPresent())
            missingFields.add("timeRange");
        if (!queryString().isPresent())
            missingFields.add("queryString");
        if (!streams().isPresent())
            missingFields.add("streams");
        if (!fieldsInOrder().isPresent())
            missingFields.add("fieldsInOrder");
        if (!sort().isPresent())
            missingFields.add("sort");

        if (!missingFields.isEmpty()) {
            String message = "Missing fields: " + missingFields;
            throw new ValidationException(message);
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder timeRange(TimeRange timeRange);

        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        @JsonProperty("query_string")
        public abstract Builder queryString(BackendQuery queryString);

        @JsonProperty("fields_in_order")
        public abstract Builder fieldsInOrder(Set<String> fieldsInOrder);

        public Builder fieldsInOrder(String... fieldsInOrder) {
            return fieldsInOrder(ImmutableSet.copyOf(fieldsInOrder));
        }

        @JsonProperty
        public abstract Builder sort(Set<Sort> sort);

        abstract MessagesRequest autoBuild();

        public MessagesRequest build() {
            return autoBuild();
        }

        @JsonCreator
        public static Builder create() {
            return new AutoValue_MessagesRequest.Builder();
        }
    }
}
