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
package org.graylog.plugins.views.search.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.export.AuditContext;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.graylog.plugins.views.audit.ViewsAuditEventTypes.MESSAGES_EXPORT_REQUESTED;
import static org.graylog.plugins.views.audit.ViewsAuditEventTypes.MESSAGES_EXPORT_SUCCEEDED;

@AutoValue
@JsonAutoDetect
public abstract class MessagesExportEvent {

    public static MessagesExportEvent requested(DateTime startTime, AuditContext context, ExportMessagesCommand command) {
        return from(startTime, context, command, MESSAGES_EXPORT_REQUESTED);
    }

    public static MessagesExportEvent succeeded(DateTime startTime, AuditContext context, ExportMessagesCommand command) {
        return from(startTime, context, command, MESSAGES_EXPORT_SUCCEEDED);
    }

    private static MessagesExportEvent from(DateTime startTime, AuditContext context, ExportMessagesCommand command, String auditType) {
        Builder builder = Builder.create()
                .userName(context.userName())
                .auditType(auditType)
                .timestamp(startTime)
                .timeRange(command.timeRange())
                .queryString(command.queryString().queryString())
                .streams(command.streams())
                .fieldsInOrder(command.fieldsInOrder());

        if (command.limit().isPresent()) {
            builder.limit(command.limit().getAsInt());
        }
        if (context.searchId().isPresent()) {
            builder.searchId(context.searchId().get());
        }
        if (context.searchTypeId().isPresent()) {
            builder.searchTypeId(context.searchTypeId().get());
        }

        return builder.build();
    }

    public abstract String userName();

    public abstract String auditType();

    public abstract DateTime timestamp();

    public abstract AbsoluteRange timeRange();

    public abstract String queryString();

    public abstract Set<String> streams();

    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract OptionalInt limit();

    public abstract Optional<String> searchId();

    public abstract Optional<String> searchTypeId();

    public abstract Builder toBuilder();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", timestamp());
        map.put("time_range", timeRange());
        map.put("query_string", queryString());
        map.put("streams", streams());
        map.put("fields", fieldsInOrder());

        limit().ifPresent(limit -> map.put("limit", limit));
        searchId().ifPresent(searchId -> map.put("search_id", searchId));
        searchTypeId().ifPresent(searchTypeId -> map.put("search_type_id", searchTypeId));

        return map;
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder userName(String userName);

        public abstract Builder auditType(String auditType);

        public abstract Builder timestamp(DateTime executionStart);

        public abstract Builder timeRange(AbsoluteRange timeRange);

        public abstract Builder queryString(String queryString);

        public abstract Builder streams(Set<String> streams);

        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public abstract Builder limit(Integer limit);

        public abstract Builder searchId(String searchId);

        public abstract Builder searchTypeId(String searchTypeId);

        abstract MessagesExportEvent autoBuild();

        public MessagesExportEvent build() {
            return autoBuild();
        }

        @JsonCreator
        public static Builder create() {
            return new AutoValue_MessagesExportEvent.Builder();
        }
    }


}
