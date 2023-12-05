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
import com.google.inject.name.Names;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.MongoDBRuleFragmentService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20220512123200_AddSimpleConditionFragments;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20220522125200_AddSetGrokToFieldsExtractorFragments;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20230613154400_AddImplicitToStringFragments;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20230720161500_AddExtractorFragments;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20230724092100_AddFieldConditions;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations.V20230915095200_AddSimpleRegex;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action.ValidAction;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action.ValidNewMessageField;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action.ValidVariables;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.condition.ValidCondition;
import org.graylog.plugins.pipelineprocessor.rulebuilder.rest.RuleBuilderResource;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.PluginModule;

public class RuleBuilderModule extends PluginModule {

    @Override
    protected void configure() {

        bind(RuleFragmentService.class).to(MongoDBRuleFragmentService.class).asEagerSingleton();

        addSystemRestResource(RuleBuilderResource.class);

        final Multibinder<Migration> migrationBinder = Multibinder.newSetBinder(binder(), Migration.class);
        migrationBinder.addBinding().to(V20220512123200_AddSimpleConditionFragments.class);
        migrationBinder.addBinding().to(V20220522125200_AddSetGrokToFieldsExtractorFragments.class);
        migrationBinder.addBinding().to(V20230613154400_AddImplicitToStringFragments.class);
        migrationBinder.addBinding().to(V20230720161500_AddExtractorFragments.class);
        migrationBinder.addBinding().to(V20230724092100_AddFieldConditions.class);
        migrationBinder.addBinding().to(V20230915095200_AddSimpleRegex.class);

        final Multibinder<Validator> condition = Multibinder.newSetBinder(binder(), Validator.class, Names.named("conditionValidators"));
        condition.addBinding().to(ValidCondition.class);

        final Multibinder<Validator> action = Multibinder.newSetBinder(binder(), Validator.class, Names.named("actionValidators"));
        action.addBinding().to(ValidAction.class);
        action.addBinding().to(ValidVariables.class);
        action.addBinding().to(ValidNewMessageField.class);

    }
}
