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
package org.graylog2.shared.messageq;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.journal.NoopJournal;
import org.graylog2.shared.messageq.localkafka.LocalKafkaMessageQueueAcknowledger;
import org.graylog2.shared.messageq.localkafka.LocalKafkaMessageQueueReader;
import org.graylog2.shared.messageq.localkafka.LocalKafkaMessageQueueWriter;
import org.graylog2.shared.messageq.noop.NoopMessageQueueAcknowledger;
import org.graylog2.shared.messageq.noop.NoopMessageQueueReader;
import org.graylog2.shared.messageq.noop.NoopMessageQueueWriter;

public class MessageQueueModule extends PluginModule {
    public static String NOOP_IMPLEMENTATION = "noop";
    public static String DISK_IMPLEMENTATION = "disk";

    private final Configuration configuration;

    public MessageQueueModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        addMessageQueueImplementation(NOOP_IMPLEMENTATION, NoopMessageQueueReader.class, NoopMessageQueueWriter.class,
                NoopMessageQueueAcknowledger.class);
        addMessageQueueImplementation(DISK_IMPLEMENTATION, LocalKafkaMessageQueueReader.class,
                LocalKafkaMessageQueueWriter.class, LocalKafkaMessageQueueAcknowledger.class);

        if (configuration.getMessageJournalMode().equals(DISK_IMPLEMENTATION)) {
            install(new KafkaJournalModule());
            serviceBinder().addBinding().to(KafkaJournal.class).in(Scopes.SINGLETON);
        } else {
            binder().bind(Journal.class).to(NoopJournal.class).in(Scopes.SINGLETON);
            serviceBinder().addBinding().to(NoopJournal.class).in(Scopes.SINGLETON);
        }

        bind(MessageQueueReader.class)
                .toProvider(new TypeLiteral<MessageQueueImplProvider<MessageQueueReader>>() {})
                .in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class)
                .toProvider(new TypeLiteral<MessageQueueImplProvider<MessageQueueWriter>>() {})
                .in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class)
                .toProvider(new TypeLiteral<MessageQueueImplProvider<MessageQueueAcknowledger>>() {})
                .in(Scopes.SINGLETON);
    }
}
