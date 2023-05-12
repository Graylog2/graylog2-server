/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog2.migrations.Migration;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20220512123200_AddSystemRuleFragments extends Migration {

    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20220512123200_AddSystemRuleFragments(RuleFragmentService ruleFragmentService) {
        this.ruleFragmentService = ruleFragmentService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-12T12:32:00Z");
    }

    @Override
    public void upgrade() {
        ruleFragmentService.delete("has_field_equals");
        ruleFragmentService.save(
                RuleFragment.builder()
                        .fragment("( hasField(\"{field}\") && to_string($message.{field}) == \"{fieldValue}\" )")
                        .descriptor(FunctionDescriptor.builder()
                                .name("has_field_equals")
                                .params(ImmutableList.of(
                                        string("field").build(),
                                        string("fieldValue").build()
                                ))
                                .returnType(Boolean.class)
                                .ruleBuilderEnabled()
                                .build())
                        .isCondition()
                        .build()
        );
    }
}
