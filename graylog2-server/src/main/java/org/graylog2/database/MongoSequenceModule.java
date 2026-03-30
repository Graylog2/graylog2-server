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
package org.graylog2.database;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice module for {@link MongoSequenceService}. Binds the service as a singleton
 * and initializes the empty {@link SequenceTopics} multibinder. Other modules
 * (including plugin modules) add topic names to this multibinder.
 */
public class MongoSequenceModule extends AbstractModule {
    @Override
    protected void configure() {
        // Initialize the multibinder (even if no topics are registered in this module)
        Multibinder.newSetBinder(binder(), String.class, SequenceTopics.class);

        bind(MongoSequenceService.class);
    }
}
