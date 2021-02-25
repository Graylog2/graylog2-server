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

import com.google.inject.Injector;
import com.google.inject.Scopes;
import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.journal.LocalKafkaJournalModule;
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

    public MessageQueueModule(Injector configInjector) {
        super(configInjector);
    }

    @Override
    protected void configure() {
        Configuration config = getConfigInjector().getInstance(Configuration.class);
        installMessageQueueImplementation(
                NOOP_JOURNAL_MODE,
                NoopMessageQueueReader.class,
                NoopMessageQueueWriter.class,
                NoopMessageQueueAcknowledger.class);

        installMessageQueueImplementation(
                DISK_JOURNAL_MODE,
                LocalKafkaMessageQueueReader.class,
                LocalKafkaMessageQueueWriter.class,
                LocalKafkaMessageQueueAcknowledger.class);

        if (config.getMessageJournalMode().equals(DISK_JOURNAL_MODE)) {
            install(new LocalKafkaJournalModule());
            serviceBinder().addBinding().to(LocalKafkaJournal.class).in(Scopes.SINGLETON);
        } else {
            binder().bind(Journal.class).to(NoopJournal.class).in(Scopes.SINGLETON);
            serviceBinder().addBinding().to(NoopJournal.class).in(Scopes.SINGLETON);
        }
    }
}
