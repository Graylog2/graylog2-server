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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class V20230915095200_AddSimpleRegex extends Migration {

    private static final Logger log = LoggerFactory.getLogger(V20230915095200_AddSimpleRegex.class);
    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20230915095200_AddSimpleRegex(RuleFragmentService ruleFragmentService) {
        this.ruleFragmentService = ruleFragmentService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-09-15T09:52:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding simple regex fragments via migration");

        ruleFragmentService.upsert(createSimpleRegex());

        log.debug("simple regex fragments were successfully added");
    }

    static RuleFragment createSimpleRegex() {
        String resultvariable = "gl2_fragment_regex_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let %resultvar% = regex(
                          pattern: ${pattern},
                          value: ${value}<#if group_names??>,
                          group_names: ${group_names}
                          </#if>
                        );""".replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("regex_groups")
                        .params(ImmutableList.of(
                                string("pattern", Pattern.class).transform(Pattern::compile).description("The regular expression to match against 'value', uses Java regex syntax").build(),
                                string("value").ruleBuilderVariable().description("The string to match the pattern against").build(),
                                type("group_names", List.class).optional().description("List of names to use for matcher groups").build()
                        ))
                        .returnType(Map.class)
                        .description("Match a string with a regular expression (Java syntax) and return matcher groups")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Get regex groups")
                        .ruleBuilderTitle("Get regex matcher groups for '${pattern}' against '${value}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }

}
