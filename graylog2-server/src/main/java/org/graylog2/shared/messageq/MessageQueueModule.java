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
    public static String DISK_JOURNAL_MODE = "disk";
    public static String NOOP_JOURNAL_MODE = "noop";

    private final Configuration configuration;

    public MessageQueueModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        installMessageQueueImplementation(
                configuration,
                NOOP_JOURNAL_MODE,
                NoopMessageQueueReader.class,
                NoopMessageQueueWriter.class,
                NoopMessageQueueAcknowledger.class);

        installMessageQueueImplementation(
                configuration,
                DISK_JOURNAL_MODE,
                LocalKafkaMessageQueueReader.class,
                LocalKafkaMessageQueueWriter.class,
                LocalKafkaMessageQueueAcknowledger.class);

        if (configuration.getEffectiveMessageJournalMode().equals(DISK_JOURNAL_MODE)) {
            install(new KafkaJournalModule());
            serviceBinder().addBinding().to(KafkaJournal.class).in(Scopes.SINGLETON);
        } else {
            binder().bind(Journal.class).to(NoopJournal.class).in(Scopes.SINGLETON);
            serviceBinder().addBinding().to(NoopJournal.class).in(Scopes.SINGLETON);
        }
    }
}
