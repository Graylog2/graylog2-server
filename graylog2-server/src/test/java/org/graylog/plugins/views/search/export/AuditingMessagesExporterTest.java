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
        sut = new AuditingMessagesExporter(eventBus, userName, mock(MessagesExporter.class));
        nowAtStart = DateTime.now(DateTimeZone.UTC);
        nowAtEnd = DateTime.now(DateTimeZone.UTC);
        sut.startedAt = () -> nowAtStart;
        sut.finishedAt = () -> nowAtEnd;
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

    private MessagesExportEvent exportWithExpectedAuditEvent(ExportMessagesCommand command, int eventPosition) {
        sut.export(command, chunk -> {
        });

        ArgumentCaptor<MessagesExportEvent> eventCaptor = ArgumentCaptor.forClass(MessagesExportEvent.class);

        //noinspection UnstableApiUsage
        verify(eventBus, times(2)).post(eventCaptor.capture());

        return eventCaptor.getAllValues().get(eventPosition);
    }
}
