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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import com.google.inject.multibindings.OptionalBinder;
import org.graylog.plugins.pipelineprocessor.db.PaginatedPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PaginatedRuleService;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.plugin.PluginModule;

public class MongoDbServicesModule extends PluginModule {
    @Override
    protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PipelineService.class)
                .setDefault().to(MongoDbPipelineService.class);
        OptionalBinder.newOptionalBinder(binder(), RuleService.class)
                .setDefault().to(MongoDbRuleService.class);
        OptionalBinder.newOptionalBinder(binder(), PaginatedPipelineService.class)
                .setDefault().to(PaginatedMongoDbPipelineService.class);
        OptionalBinder.newOptionalBinder(binder(), PaginatedRuleService.class)
                .setDefault().to(PaginatedMongoDbRuleService.class);
        bind(PipelineStreamConnectionsService.class).to(MongoDbPipelineStreamConnectionsService.class);
    }
}
