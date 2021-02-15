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

import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.messageq.localkafka.LocalKafkaMessageQueueModule;
import org.graylog2.shared.messageq.noop.NoopMessagequeueModule;

public class MessageQueueModule extends PluginModule {
    private final Configuration configuration;

    public MessageQueueModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        if (configuration.isMessageJournalEnabled()) {
            switch (configuration.getMessageJournalMode()) {
                case DISK:
                    install(new LocalKafkaMessageQueueModule());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported journal <" + configuration.getMessageJournalMode() + ">");
            }
        } else {
            install(new NoopMessagequeueModule());
        }
    }

}
