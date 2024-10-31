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
package org.graylog2.outputs.filter;

import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.pipelineprocessor.functions.ProcessorFunctionsModule;
import org.graylog2.outputs.filter.functions.RemoveFromStreamDestination;

public class OutputFilterModule extends ProcessorFunctionsModule {
    @Override
    protected void configure() {
        bind(OutputFilter.class).to(PipelineRuleOutputFilter.class).in(Scopes.SINGLETON);

        bind(PipelineRuleOutputFilterStateUpdater.class).in(Scopes.SINGLETON);
        install(new FactoryModuleBuilder().build(PipelineRuleOutputFilterState.Factory.class));

        addInternalMessageProcessorFunction(RemoveFromStreamDestination.NAME, RemoveFromStreamDestination.class);
    }
}
