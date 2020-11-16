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
package org.graylog.plugins.views.search.export;

import com.google.common.eventbus.EventBus;
import org.graylog.plugins.views.search.events.MessagesExportEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AuditingMessagesExporterTest {
    private static final String userName = "peterchen";
    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus;
    private AuditingMessagesExporter sut;
    private DateTime nowAtStart;
    private DateTime nowAtEnd;

    @BeforeEach
    void setUp() {
        //noinspection UnstableApiUsage
        eventBus = mock(EventBus.class);
        sut = sutWithContext(new AuditContext(userName, null, null));
        nowAtStart = DateTime.now(DateTimeZone.UTC);
        nowAtEnd = DateTime.now(DateTimeZone.UTC);
        sut.startedAt = () -> nowAtStart;
        sut.finishedAt = () -> nowAtEnd;
    }

    private AuditingMessagesExporter sutWithContext(AuditContext context) {
        return new AuditingMessagesExporter(context, eventBus, mock(MessagesExporter.class));
    }

    @Test
    void sendsAuditEventWhenStarted() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults();

        MessagesExportEvent event = exportWithExpectedAuditEvent(command, 0);

        assertAll("should have sent event",
                () -> assertThat(event.userName()).isEqualTo("peterchen"),
                () -> assertThat(event.timeRange()).isEqualTo(command.timeRange()),
                () -> assertThat(event.timestamp()).isEqualTo(nowAtStart),
                () -> assertThat(event.queryString()).isEqualTo(command.queryString().queryString()),
                () -> assertThat(event.streams()).isEqualTo(command.streams()),
                () -> assertThat(event.fieldsInOrder()).isEqualTo(command.fieldsInOrder())
        );
    }

    @Test
    void sendsAuditEventWhenFinished() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults();

        MessagesExportEvent event = exportWithExpectedAuditEvent(command, 1);

        assertAll("should have sent event",
                () -> assertThat(event.userName()).isEqualTo("peterchen"),
                () -> assertThat(event.timeRange()).isEqualTo(command.timeRange()),
                () -> assertThat(event.timestamp()).isEqualTo(nowAtEnd),
                () -> assertThat(event.queryString()).isEqualTo(command.queryString().queryString()),
                () -> assertThat(event.streams()).isEqualTo(command.streams()),
                () -> assertThat(event.fieldsInOrder()).isEqualTo(command.fieldsInOrder()),
                () -> assertThat(event.limit()).isEmpty()
        );
    }

    @Test
    void auditEventHasLimitIfDefined() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults().toBuilder().limit(5).build();

        MessagesExportEvent event = exportWithExpectedAuditEvent(command, 1);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(event.limit().getAsInt()).isEqualTo(5);
    }

    @Test
    void auditEventHasSearchIdIfDefined() {
        sut = sutWithContext(new AuditContext(userName, "search-id", null));

        MessagesExportEvent event = exportWithExpectedAuditEvent(ExportMessagesCommand.withDefaults(), 1);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(event.searchId()).contains("search-id");
    }

    @Test
    void auditEventHasSearchTypeIdIfDefined() {
        sut = sutWithContext(new AuditContext(userName, "search-id", "search-type-id"));

        MessagesExportEvent event = exportWithExpectedAuditEvent(ExportMessagesCommand.withDefaults(), 1);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(event.searchTypeId()).contains("search-type-id");
    }

    private MessagesExportEvent exportWithExpectedAuditEvent(ExportMessagesCommand command, int eventPosition) {
        sut.export(command, chunk -> {
        });

        ArgumentCaptor<MessagesExportEvent> eventCaptor = ArgumentCaptor.forClass(MessagesExportEvent.class);

        //noinspection UnstableApiUsage
        verify(eventBus, times(2)).post(eventCaptor.capture());

        return eventCaptor.getAllValues().get(eventPosition);
    }
}
