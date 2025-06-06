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
package org.graylog2.streams;

import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.streams.filters.DestinationFilterCreationValidator;
import org.graylog2.streams.input.StreamRuleInputsProvider;
import org.graylog2.streams.input.StreamRuleServerInputsProvider;

public class StreamsModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(StreamRuleService.class).to(StreamRuleServiceImpl.class);
        bind(StreamService.class).to(StreamServiceImpl.class);

        Multibinder<StreamRuleInputsProvider> uriBinder = Multibinder.newSetBinder(binder(), StreamRuleInputsProvider.class);
        uriBinder.addBinding().to(StreamRuleServerInputsProvider.class);

        // The Set<StreamDeletionGuard> binder must be explicitly initialized to avoid an initialization error when
        // no values are bound.
        streamDeletionGuardBinder();
        OptionalBinder.newOptionalBinder(binder(), DestinationFilterCreationValidator.class);
    }
}
