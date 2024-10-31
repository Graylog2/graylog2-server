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
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20220512123200_AddSimpleConditionFragments extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20220512123200_AddSimpleConditionFragments.class);
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
        ruleFragmentService.deleteAll();
        log.debug("Migrating simple condition fragments for the rule builder ui");

        ruleFragmentService.upsert(createHasFieldEqualsFragment());
        ruleFragmentService.upsert(createHasFieldGreateOrEqualFragment());
        ruleFragmentService.upsert(createHasFieldLessOrEqualFragment());

        log.debug("has_field_equals, has_field_greater_or_equal, has_field_less_or_equal fragments were successfully added");
    }

    private RuleFragment createHasFieldLessOrEqualFragment() {
        return RuleFragment.builder()
                .fragment("( has_field(${field}) && to_long($message.${field}) <= ${fieldValue} )")
                .descriptor(FunctionDescriptor.builder()
                        .name("has_field_less_or_equal")
                        .params(ImmutableList.of(
                                string("field").description("Message field to check against").build(),
                                integer("fieldValue").description("Field value to check for").build()
                        ))
                        .returnType(Boolean.class)
                        .description("Checks if the message has a field and if this field's numeric value is less than or equal to the given fieldValue")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field <=")
                        .ruleBuilderTitle("Field '${field}' less than or equal '${fieldValue}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.NUMBER)
                        .build())
                .isCondition()
                .build();
    }

    private RuleFragment createHasFieldGreateOrEqualFragment() {
        return RuleFragment.builder()
                .fragment("( has_field(${field}) && to_long($message.${field}) >= ${fieldValue} )")
                .descriptor(FunctionDescriptor.builder()
                        .name("has_field_greater_or_equal")
                        .params(ImmutableList.of(
                                string("field").description("Message field to check against").build(),
                                integer("fieldValue").description("Field value to check for").build()
                        ))
                        .returnType(Boolean.class)
                        .description("Checks if the message has a field and if this field's numeric value is greater than or equal to the given fieldValue")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field >=")
                        .ruleBuilderTitle("Field '${field}' greater than or equal '${fieldValue}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.NUMBER)
                        .build())
                .isCondition()
                .build();
    }

    private RuleFragment createHasFieldEqualsFragment() {
        return RuleFragment.builder()
                .fragment("""
                        ( has_field(${field}) &&
                        <#if caseInsensitive!false>
                        lowercase(to_string($message.${field})) == lowercase(${fieldValue})
                        <#else>
                        to_string($message.${field}) == ${fieldValue}
                        </#if>
                        )
                        """)
                .descriptor(FunctionDescriptor.builder()
                        .name("has_field_equals")
                        .params(ImmutableList.of(
                                string("field").description("Message field to check against").build(),
                                string("fieldValue").description("Field value to check for").build(),
                                bool("caseInsensitive").optional().description("Ignore case").build()
                        ))
                        .returnType(Boolean.class)
                        .description("Checks if the message has a field and if this field's string value is equal to the given fieldValue")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field equals")
                        .ruleBuilderTitle("Field '${field}' equals '${fieldValue}' <#if caseInsensitive??>(case insensitive: ${caseInsensitive?c})</#if>")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.STRING)
                        .build())
                .isCondition()
                .build();
    }

}
