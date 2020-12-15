/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
public abstract class Query {
    private final String TIMESTAMP_FIELD = "timestamp";
    private final List<String> DEFAULT_FIELDS = ImmutableList.of(TIMESTAMP_FIELD, "source", "message");

    abstract String rangeType();
    abstract Optional<String> fields();
    public abstract String query();
    public abstract Optional<String> streamId();

    public abstract TimeRange toTimeRange();

    public MessagesWidget toMessagesWidget(String messageListId) {
        final List<String> usedFieldsWithoutMessage = fieldsList().stream()
                .filter(field -> !field.equals("message"))
                .collect(Collectors.toList());
        final boolean showMessageRow = fieldsList().contains("message");

        return MessagesWidget.create(messageListId, usedFieldsWithoutMessage, showMessageRow);
    }

    private List<String> addTimestampFieldIfMissing(List<String> fields) {
        if (!fields.contains(TIMESTAMP_FIELD)) {
            final List<String> fieldsWithTimestamp = new ArrayList<>(fields.size() + 1);
            fieldsWithTimestamp.add(TIMESTAMP_FIELD);
            fieldsWithTimestamp.addAll(fields);
            return fieldsWithTimestamp;
        }
        return fields;
    }

    private List<String> fieldsList() {
        //noinspection UnstableApiUsage
        return fields()
                .filter(fields -> !fields.trim().isEmpty())
                .map(fields -> Splitter.on(",").splitToList(fields))
                .map(this::addTimestampFieldIfMissing)
                .orElse(DEFAULT_FIELDS);
    }
}
