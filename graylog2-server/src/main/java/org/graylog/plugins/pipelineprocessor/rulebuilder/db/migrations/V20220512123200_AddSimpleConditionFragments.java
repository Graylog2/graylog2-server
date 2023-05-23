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
package org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog2.migrations.Migration;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20220512123200_AddSimpleConditionFragments extends Migration {

    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20220512123200_AddSimpleConditionFragments(RuleFragmentService ruleFragmentService) {
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
                        .fragment("( hasField(\"${field}\") && to_string($message.${field}) == \"${fieldValue}\" )")
                        .descriptor(FunctionDescriptor.builder()
                                .name("has_field_equals")
                                .params(ImmutableList.of(
                                        string("field").description("Message field to check against").build(),
                                        string("fieldValue").description("Field value to check for").build()
                                ))
                                .returnType(Boolean.class)
                                .description("Checks if the message has a field and if this field's string value is equal to the given fieldValue")
                                .ruleBuilderEnabled()
                                .ruleBuilderTitle("Field '${field}' equals '${fieldValue}'")
                                .build())
                        .isCondition()
                        .build()
        );
        ruleFragmentService.delete("has_field_greater_or_equal");
        ruleFragmentService.save(
                RuleFragment.builder()
                        .fragment("( hasField(\"${field}\") && to_double($message.${field}) >= ${fieldValue} )")
                        .descriptor(FunctionDescriptor.builder()
                                .name("has_field_greater_or_equal")
                                .params(ImmutableList.of(
                                        string("field").description("Message field to check against").build(),
                                        integer("fieldValue").description("Field value to check for").build()
                                ))
                                .returnType(Boolean.class)
                                .description("Checks if the message has a field and if this field's numeric value is greater than or equal to the given fieldValue")
                                .ruleBuilderEnabled()
                                .ruleBuilderTitle("Field '${field}' greater than or equal '${fieldValue}'")
                                .build())
                        .isCondition()
                        .build()
        );
        ruleFragmentService.delete("has_field_less_or_equal");
        ruleFragmentService.save(
                RuleFragment.builder()
                        .fragment("( hasField(\"${field}\") && to_double($message.${field}) <= ${fieldValue} )")
                        .descriptor(FunctionDescriptor.builder()
                                .name("has_field_less_or_equal")
                                .params(ImmutableList.of(
                                        string("field").description("Message field to check against").build(),
                                        integer("fieldValue").description("Field value to check for").build()
                                ))
                                .returnType(Boolean.class)
                                .description("Checks if the message has a field and if this field's numeric value is less than or equal to the given fieldValue")
                                .ruleBuilderEnabled()
                                .ruleBuilderTitle("Field '${field}' less than or equal '${fieldValue}'")
                                .build())
                        .isCondition()
                        .build()
        );
    }
}
