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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.MessagesWidget;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "rangeType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteTimeRangeQuery.class, name = AbsoluteTimeRangeQuery.type),
        @JsonSubTypes.Type(value = KeywordTimeRangeQuery.class, name = KeywordTimeRangeQuery.type),
        @JsonSubTypes.Type(value = RelativeTimeRangeQuery.class, name = RelativeTimeRangeQuery.type)
})
public interface Query {
    String TIMESTAMP_FIELD = "timestamp";
    List<String> DEFAULT_FIELDS = ImmutableList.of(TIMESTAMP_FIELD, "source", "message");

    String rangeType();
    Optional<String> fields();
    String query();
    Optional<String> streamId();

    TimeRange toTimeRange();

    default MessagesWidget toMessagesWidget(String messageListId) {
        final List<String> usedFieldsWithoutMessage = fieldsList().stream()
                .filter(field -> !field.equals("message"))
                .collect(Collectors.toList());
        final boolean showMessageRow = fieldsList().contains("message");

        return MessagesWidget.create(messageListId, usedFieldsWithoutMessage, showMessageRow);
    }

    default List<String> fieldsList() {
        return fields()
                .filter(fields -> !fields.trim().isEmpty())
                .map(fields -> Splitter.on(",").splitToList(fields))
                .map(fields -> {
                    if (!fields.contains(TIMESTAMP_FIELD)) {
                        final List<String> fieldsWithTimestamp = new ArrayList<>(fields.size() + 1);
                        fieldsWithTimestamp.add(TIMESTAMP_FIELD);
                        fieldsWithTimestamp.addAll(fields);
                        return fieldsWithTimestamp;
                    }
                    return fields;
                })
                .orElse(DEFAULT_FIELDS);
    }
}
