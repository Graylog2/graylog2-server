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
package org.graylog.plugins.pipelineprocessor.rulebuilder;

import com.google.inject.multibindings.Multibinder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.MongoDBRuleFragmentService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20220512123200_AddSystemRuleFragments;
import org.graylog.plugins.pipelineprocessor.rulebuilder.rest.RuleBuilderResource;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.PluginModule;

public class RuleBuilderModule extends PluginModule {

    @Override
    protected void configure() {

        bind(RuleFragmentService.class).to(MongoDBRuleFragmentService.class).asEagerSingleton();

        addSystemRestResource(RuleBuilderResource.class);

        final Multibinder<Migration> binder = Multibinder.newSetBinder(binder(), Migration.class);
        binder.addBinding().to(V20220512123200_AddSystemRuleFragments.class);

    }
}
