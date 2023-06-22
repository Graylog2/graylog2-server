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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20220522125200_AddSetGrokToFieldsExtractorFragments extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20220522125200_AddSetGrokToFieldsExtractorFragments.class);
    private final RuleFragmentService ruleFragmentService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20220522125200_AddSetGrokToFieldsExtractorFragments(RuleFragmentService ruleFragmentService, ClusterConfigService clusterConfigService) {
        this.ruleFragmentService = ruleFragmentService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-22T12:52:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding set_grok_to_fields extractor fragments via migration");
        if (Objects.nonNull(clusterConfigService.get(MigrationCompleted.class))) {
            log.debug("Migration already completed!");
            return;

        }

        RuleFragment setGrokTFieldsFragment = createSetGrokToFieldsFragment();
        ruleFragmentService.get(setGrokTFieldsFragment.getName()).ifPresent(fragment -> {
            ruleFragmentService.delete(fragment.getName());
        });

        ruleFragmentService.save(setGrokTFieldsFragment);
        clusterConfigService.write(new MigrationCompleted());
        log.debug("set_grok_to_fields fragment was successfully added");
    }

    private static RuleFragment createSetGrokToFieldsFragment() {
        return RuleFragment.builder()
                .fragment("""
                        let gl2_fragment_grok_results = grok(
                          pattern: "${grokPattern}",
                          value: to_string($message.${field}),
                          only_named_captures: ${grokNamedOnly?c}
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
                        .returnType(Void.class)
                        .description("Match grok pattern and set fields")
                        .ruleBuilderEnabled()
                        .ruleBuilderTitle("Match grok pattern on field '${field}' and set fields")
                        .build())
                .build();
    }

    private void addFragment(RuleFragment ruleFragment) {
        Optional<RuleFragment> existingFragment = ruleFragmentService.get(ruleFragment.getName());
        existingFragment.ifPresent(fragment -> ruleFragmentService.delete(fragment.getName()));
        ruleFragmentService.save(ruleFragment);
    }

    public record MigrationCompleted() {}
}
