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
package org.graylog2.datatiering;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringOrchestrator;
import org.graylog2.datatiering.rotation.DataTierRotation;
import org.graylog2.plugin.inject.Graylog2Module;

public class DataTieringModule extends Graylog2Module {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(DataTierRotation.Factory.class));
        OptionalBinder.newOptionalBinder(binder(), DataTieringOrchestrator.class).setDefault().to(HotOnlyDataTieringOrchestrator.class);
        registerJacksonSubtype(HotOnlyDataTieringConfig.class, HotOnlyDataTieringConfig.TYPE);
    }
}
