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
package org.graylog2.shared.messageq.localkafka;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.journal.KafkaMessageQueueReader;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueReader;
import org.graylog2.shared.messageq.MessageQueueWriter;

public class LocalKafkaMessageQueueModule extends Graylog2Module {
    @Override
    protected void configure() {
        install(new KafkaJournalModule());
        bind(MessageQueueReader.class).to(KafkaMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(LocalKafkaMessageQueueWriter.class).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(LocalKafkaMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        final Multibinder<Service> serviceBinder = serviceBinder();
        serviceBinder.addBinding().to(KafkaMessageQueueReader.class).in(Scopes.SINGLETON);
        serviceBinder.addBinding().to(KafkaJournal.class).in(Scopes.SINGLETON);
    }
}
