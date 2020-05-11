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
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_CHUNK_SIZE;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_FIELDS;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_QUERY;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_STREAMS;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.defaultTimeRange;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;

@AutoValue
@JsonDeserialize(builder = MessagesRequest.Builder.class)
public abstract class MessagesRequest {

    public abstract TimeRange timeRange();

    public abstract ElasticsearchQueryString queryString();

    public abstract Set<String> streams();

    @NotEmpty
    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract int chunkSize();

    @Positive
    public abstract OptionalInt limit();

    public static MessagesRequest withDefaults() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("timerange")
        public abstract Builder timeRange(TimeRange timeRange);

        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        @JsonProperty("query_string")
        public abstract Builder queryString(ElasticsearchQueryString queryString);

        @JsonProperty("fields_in_order")
        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public Builder fieldsInOrder(String... fieldsInOrder) {
            return fieldsInOrder(linkedHashSetOf(fieldsInOrder));
        }

        @JsonProperty("chunk_size")
        public abstract Builder chunkSize(int chunkSize);

        @JsonProperty
        public abstract Builder limit(Integer limit);

        abstract MessagesRequest autoBuild();

        public MessagesRequest build() {
            return autoBuild();
        }

        @JsonCreator
        public static Builder create() {
            return new AutoValue_MessagesRequest.Builder()
                    .timeRange(defaultTimeRange())
                    .streams(DEFAULT_STREAMS)
                    .queryString(DEFAULT_QUERY)
                    .fieldsInOrder(DEFAULT_FIELDS)
                    .chunkSize(DEFAULT_CHUNK_SIZE);
        }
    }
}
