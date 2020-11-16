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
package org.graylog.plugins.pipelineprocessor;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.functions.ProcessorFunctionsModule;
import org.graylog.plugins.pipelineprocessor.periodical.LegacyDefaultStreamMigration;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog2.plugin.PluginModule;

public class PipelineProcessorModule extends PluginModule {
    @Override
    protected void configure() {
        addPeriodical(LegacyDefaultStreamMigration.class);

        addMessageProcessor(PipelineInterpreter.class, PipelineInterpreter.Descriptor.class);
        addPermissions(PipelineRestPermissions.class);

        registerRestControllerPackage(getClass().getPackage().getName());

        install(new ProcessorFunctionsModule());

        installSearchResponseDecorator(searchResponseDecoratorBinder(),
                PipelineProcessorMessageDecorator.class,
                PipelineProcessorMessageDecorator.Factory.class);

        install(new FactoryModuleBuilder().build(PipelineInterpreter.State.Factory.class));

        addAuditEventTypes(PipelineProcessorAuditEventTypes.class);
    }
}
