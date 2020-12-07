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
package org.graylog2.shared.messageq.pulsar;

import com.google.inject.Scopes;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.NoopJournal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueWriter;

public class PulsarMessageQueueModule extends PluginModule {
    @Override
    protected void configure() {
        serviceBinder().addBinding().to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
        serviceBinder().addBinding().to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(PulsarMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        binder().bind(Journal.class).to(NoopJournal.class).in(Scopes.SINGLETON);
    }
}
