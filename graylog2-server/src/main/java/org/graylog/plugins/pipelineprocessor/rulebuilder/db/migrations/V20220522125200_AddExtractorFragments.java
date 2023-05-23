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

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20220522125200_AddExtractorFragments extends Migration {

    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20220522125200_AddExtractorFragments(RuleFragmentService ruleFragmentService) {
        this.ruleFragmentService = ruleFragmentService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-22T12:52:00Z");
    }

    @Override
    public void upgrade() {
        ruleFragmentService.delete("set_grok_to_fields");
        ruleFragmentService.save(
                RuleFragment.builder()
                        .fragment("""
                                let gl2_fragment_grok_results = grok(
                                  pattern: "${grokPattern}",
                                  value: to_string($message.${field}),
                                  only_named_captures: ${grokNamedOnly!"false"}
                                );
                                set_fields(
                                  fields: gl2_fragment_grok_results,
                                  prefix: "${prefix!""}",
                                  suffix: "${suffix!""}"
                                );""")
                        .descriptor(FunctionDescriptor.builder()
                                .name("set_grok_to_fields")
                                .params(ImmutableList.of(
                                        string("field").description("Field to extract and apply grok pattern to").build(),
                                        string("grokPattern").description("Grok pattern to apply").build(),
                                        bool("grokNamedOnly").optional().description("Set to true to only set fields for named captures").build(),
                                        string("prefix").optional().description("Prefix for created fields").build(),
                                        string("suffix").optional().description("Suffix for created fields").build()
                                ))
                                .returnType(String.class)
                                .description("Match grok pattern and set fields")
                                .ruleBuilderEnabled()
                                .ruleBuilderTitle("Match grok pattern and set fields")
                                .build())
                        .build()
        );
    }
}
