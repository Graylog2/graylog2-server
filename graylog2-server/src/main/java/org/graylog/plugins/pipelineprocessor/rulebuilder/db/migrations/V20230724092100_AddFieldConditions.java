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
import java.util.Arrays;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20230724092100_AddFieldConditions extends Migration {

    private static final Logger log = LoggerFactory.getLogger(V20230724092100_AddFieldConditions.class);
    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20230724092100_AddFieldConditions(RuleFragmentService ruleFragmentService) {
        this.ruleFragmentService = ruleFragmentService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-07-24T09:21:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding field condition fragments via migration");
        String[] noConversionTypes = {"collection", "list", "not_null", "null"};
        Arrays.stream(noConversionTypes).forEach(type -> ruleFragmentService.upsert(createCheckFieldTypeNoConversion(type)));
        String[] conversionTypes = {"map", "string", "url"};
        Arrays.stream(conversionTypes).forEach(type -> ruleFragmentService.upsert(createCheckFieldType(type)));
        String[] conversionParamTypes = {"bool", "double", "long", "ip", "number"};
        Arrays.stream(conversionParamTypes).forEach(type -> ruleFragmentService.upsert(createCheckFieldTypeConversionParam(type)));
        ruleFragmentService.upsert(createCheckDateField());
        ruleFragmentService.upsert(createCIDRMatchField());
        ruleFragmentService.upsert(createStringContainsField());
        ruleFragmentService.upsert(createStringEndsWithField());
        ruleFragmentService.upsert(createStringStartsWithField());
        ruleFragmentService.upsert(createGrokMatchesField());

        log.debug("field condition fragments were successfully added");
    }

    RuleFragment createCheckFieldTypeNoConversion(String type) {
        return RuleFragment.builder()
                .fragment("is_%type%($message.${field})"
                        .replace("%type%", type)
                )
                .descriptor(FunctionDescriptor.builder()
                        .name("field_" + type)
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks whether the value in the given field is a " + type)
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field is " + type)
                        .ruleBuilderTitle("Check if value in '${field}' is a " + type)
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createCheckFieldType(String type) {
        return RuleFragment.builder()
                .fragment("<#if attemptConversion!false>is_%type%(to_%type%($message.${field}))<#else>is_%type%($message.${field})</#if>"
                        .replace("%type%", type)
                )
                .descriptor(FunctionDescriptor.builder()
                        .name("field_" + type)
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                bool("attemptConversion").optional().description("If set the check will also try if the field could be converted to a " + type + " using the to_" + type + " method").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks whether the value in the given field is a " + type)
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field is " + type)
                        .ruleBuilderTitle("Check if value in '${field}' is a " + type)
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createCheckFieldTypeConversionParam(String type) {
        return RuleFragment.builder()
                .fragment(("is_%type%(" +
                        "  value: $message.${field}<#if attemptConversion!false>," +
                        "  attemptConversion: true</#if>" +
                        ")")
                        .replace("%type%", type)
                )
                .descriptor(FunctionDescriptor.builder()
                        .name("field_" + type)
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                bool("attemptConversion").optional().description("If set the check will also try if the field's string representation represents a " + type).build()
                        ))
                        .returnType(Void.class)
                        .description("Checks whether the value in the given field is a " + type)
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field is " + type)
                        .ruleBuilderTitle("Check if value in '${field}' is a " + type)
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createCheckDateField() {
        return RuleFragment.builder()
                .fragment("""
                        is_date(
                          <#if pattern??>
                          value: parse_date(to_string($message.${field}), ${pattern})
                          <#else>
                          value: $message.${field}
                          </#if>
                        )""")
                .descriptor(FunctionDescriptor.builder()
                        .name("field_date")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("pattern").optional().description("Date pattern (see parse_date)").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks whether the value in the given field is a date, optionally by first trying to parse it.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field is date")
                        .ruleBuilderTitle("Check if value in '${field}' is a date")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createCIDRMatchField() {
        return RuleFragment.builder()
                .fragment("""
                        (
                        is_ip(to_ip($message.${field})) &&
                        cidr_match(
                          ip: to_ip($message.${field}),
                          cidr: ${cidr}
                        )
                        )""")
                .descriptor(FunctionDescriptor.builder()
                        .name("field_cidr")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("cidr").description("Date pattern (see parse_date)").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks whether the value in the given field is an IP and matches the given cidr subnet mask.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field matches CIDR")
                        .ruleBuilderTitle("Check if value in '${field}' is an IP and matches '${cidr}' subnet mask")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createStringContainsField() {
        return RuleFragment.builder()
                .fragment("""
                        contains(
                          value: to_string($message.${field}),
                          search: ${search}<#if ignoreCase??>,
                          ignore_case: ${ignoreCase?c}</#if>
                        )""")
                .descriptor(FunctionDescriptor.builder()
                        .name("field_contains")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("search").description("The substring to find").build(),
                                bool("ignoreCase").optional().description("Whether to search case insensitive, defaults to false").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks if the field value's string representation contains the given substring.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field contains")
                        .ruleBuilderTitle("Check if string value in '${field}' contains '${search}' <#if ignoreCase??>(ignore case: ${ignoreCase?c})</#if>")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createStringStartsWithField() {
        return RuleFragment.builder()
                .fragment("""
                        starts_with(
                          value: to_string($message.${field}),
                          prefix: ${search}<#if ignoreCase??>,
                          ignore_case: ${ignoreCase?c}</#if>
                        )""")
                .descriptor(FunctionDescriptor.builder()
                        .name("field_starts_with")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("search").description("The substring to find").build(),
                                bool("ignoreCase").optional().description("Whether to search case insensitive, defaults to false").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks if the field value's string representation starts with the given substring.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field starts with")
                        .ruleBuilderTitle("Check if string value in '${field}' starts with '${search}' <#if ignoreCase??>(ignore case: ${ignoreCase?c})</#if>")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createStringEndsWithField() {
        return RuleFragment.builder()
                .fragment("""
                        ends_with(
                          value: to_string($message.${field}),
                          suffix: ${search}<#if ignoreCase??>,
                          ignore_case: ${ignoreCase?c}</#if>
                        )""")
                .descriptor(FunctionDescriptor.builder()
                        .name("field_ends_with")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("search").description("The substring to find").build(),
                                bool("ignoreCase").optional().description("Whether to search case insensitive, defaults to false").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks if the field value's string representation ends with the given substring.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field ends with")
                        .ruleBuilderTitle("Check if string value in '${field}' ends with '${search}' <#if ignoreCase??>(ignore case: ${ignoreCase?c})</#if>")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

    RuleFragment createGrokMatchesField() {
        return RuleFragment.builder()
                .fragment("""
                        grok(
                          value: to_string($message.${field}),
                          pattern: ${pattern}
                        ).matches == true""")
                .descriptor(FunctionDescriptor.builder()
                        .name("grok_matches")
                        .params(ImmutableList.of(
                                string("field").description("Field to check").build(),
                                string("pattern").description("Grok pattern to check string against").build()
                        ))
                        .returnType(Void.class)
                        .description("Checks if the field value's string representation matches the given grok pattern.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Field matches grok")
                        .ruleBuilderTitle("Check if string value in '${field}' matches grok pattern '${pattern}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }

}
