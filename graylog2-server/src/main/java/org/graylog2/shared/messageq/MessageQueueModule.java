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
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.messageq.kafka.KafkaMessageQueueAcknowledger;
import org.graylog2.shared.messageq.kafka.KafkaMessageQueueWriter;

public class MessageQueueModule extends PluginModule {

    @Override
    protected void configure() {
        // TODO unify with JournalReaderModule

        // Journal with Pulsar
//        serviceBinder().addBinding().to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
//        serviceBinder().addBinding().to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
//        bind(MessageQueueWriter.class).to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
//        bind(MessageQueueAcknowledger.class).to(PulsarMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        // Journal with Kafka
        bind(MessageQueueWriter.class).to(KafkaMessageQueueWriter.class).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(KafkaMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        // TODO no Journal
    }

}
