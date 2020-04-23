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
package org.graylog.plugins.views.search.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class MessagesExportEvent {
    public static MessagesExportEvent from(DateTime dateTime, String userName, ExportMessagesCommand command) {
        return Builder.create()
                .userName(userName)
                .executionStart(dateTime)
                .timeRange(command.timeRange())
                .queryString(command.queryString().queryString())
                .streams(command.streams())
                .fieldsInOrder(command.fieldsInOrder())
                .build();
    }

    public abstract String userName();

    public abstract DateTime executionStart();

    public abstract AbsoluteRange timeRange();

    public abstract String queryString();

    public abstract Set<String> streams();

    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract Builder toBuilder();

    public Map<String, Object> toMap() {
        return ImmutableMap.of(
                "execution_start", executionStart(),
                "time_range", timeRange(),
                "query_string", queryString(),
                "streams", streams(),
                "fields", fieldsInOrder()
        );
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder userName(String userName);

        public abstract Builder executionStart(DateTime executionStart);

        public abstract Builder timeRange(AbsoluteRange timeRange);

        public abstract Builder queryString(String queryString);

        public abstract Builder streams(Set<String> streams);

        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        abstract MessagesExportEvent autoBuild();

        public MessagesExportEvent build() {
            return autoBuild();
        }

        @JsonCreator
        public static Builder create() {
            return new AutoValue_MessagesExportEvent.Builder().userName("TODO");
        }
    }


}
