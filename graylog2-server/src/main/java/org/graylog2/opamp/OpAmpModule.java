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
package org.graylog2.opamp;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.graylog2.opamp.transport.OpAmpAuthFilter;
import org.graylog2.opamp.transport.OpAmpHttpHandler;
import org.graylog2.opamp.transport.OpAmpWebSocketApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpAmpModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OpAmpService.class).in(Scopes.SINGLETON);
        bind(OpAmpHttpHandler.class).in(Scopes.SINGLETON);
        bind(OpAmpWebSocketApplication.class).in(Scopes.SINGLETON);
        bind(OpAmpAuthFilter.class).in(Scopes.SINGLETON);
        bind(ExecutorService.class).annotatedWith(OpAmpExecutor.class)
                .toInstance(Executors.newVirtualThreadPerTaskExecutor());
    }
}
