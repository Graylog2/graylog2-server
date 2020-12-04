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
package org.graylog2.shared.messageq.noop;

import com.google.inject.Scopes;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.NoopJournal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueReader;
import org.graylog2.shared.messageq.MessageQueueWriter;

public class NoopMessagequeueModule extends Graylog2Module {
    @Override
    protected void configure() {
        binder().bind(Journal.class).to(NoopJournal.class).in(Scopes.SINGLETON);
        bind(MessageQueueReader.class).to(NoopMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(NoopMessageQueueWriter.class).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(NoopMessageQueueAcknowledger.class).in(Scopes.SINGLETON);
        serviceBinder().addBinding().to(NoopJournal.class).in(Scopes.SINGLETON);
    }
}
