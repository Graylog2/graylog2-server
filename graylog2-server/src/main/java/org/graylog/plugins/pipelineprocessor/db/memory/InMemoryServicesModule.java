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
package org.graylog.plugins.pipelineprocessor.db.memory;

import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.plugin.PluginModule;

public class InMemoryServicesModule extends PluginModule {
    @Override
    protected void configure() {
        bind(RuleService.class).to(InMemoryRuleService.class).asEagerSingleton();
        bind(PipelineService.class).to(InMemoryPipelineService.class).asEagerSingleton();
        bind(PipelineStreamConnectionsService.class).to(InMemoryPipelineStreamConnectionsService.class).asEagerSingleton();
    }
}
