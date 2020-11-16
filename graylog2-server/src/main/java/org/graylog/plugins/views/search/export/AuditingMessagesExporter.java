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
