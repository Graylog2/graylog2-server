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
package org.graylog2.plugin;

import com.google.inject.AbstractModule;
import org.graylog2.indexer.results.DefaultResultMessageFactory;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.shared.buffers.processors.ProcessorConfiguration;

public class MessageBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(MessageFactory.class).to(DefaultMessageFactory.class).asEagerSingleton();
        bind(ResultMessageFactory.class).to(DefaultResultMessageFactory.class).asEagerSingleton();
        bind(ProcessorConfiguration.class).to(ProcessorConfiguration.class).asEagerSingleton();
    }
}
