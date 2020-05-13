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

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AuditingMessagesExporter implements MessagesExporter {
    private final AuditContext context;
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;
    private final MessagesExporter decoratedExporter;

    public Supplier<DateTime> startedAt = () -> DateTime.now(DateTimeZone.UTC);
    public Supplier<DateTime> finishedAt = () -> DateTime.now(DateTimeZone.UTC);

    public AuditingMessagesExporter(AuditContext context, @SuppressWarnings("UnstableApiUsage") EventBus eventBus, MessagesExporter decoratedExporter) {
        this.context = context;
        this.eventBus = eventBus;
        this.decoratedExporter = decoratedExporter;
    }

    @Override
    public void export(ExportMessagesCommand command, Consumer<SimpleMessageChunk> chunkForwarder) {
        post(MessagesExportEvent.requested(startedAt.get(), context, command));

        decoratedExporter.export(command, chunkForwarder);

        post(MessagesExportEvent.succeeded(finishedAt.get(), context, command));
    }

    private void post(Object event) {
        //noinspection UnstableApiUsage
        eventBus.post(requireNonNull(event));
    }
}
