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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20230720161500_AddExtractorFragments extends Migration {

    private static final Logger log = LoggerFactory.getLogger(V20230613154400_AddImplicitToStringFragments.class);
    private final RuleFragmentService ruleFragmentService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230720161500_AddExtractorFragments(RuleFragmentService ruleFragmentService, ClusterConfigService clusterConfigService) {
        this.ruleFragmentService = ruleFragmentService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-06-13T15:44:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding extractor fragments via migration");
        if (Objects.nonNull(clusterConfigService.get(MigrationCompleted.class))) {
            log.debug("Migration already completed!");
//            return;
        }

        addFragment(createCopyFieldExtractor());
        addFragment(createRegexExtractor());

        clusterConfigService.write(new MigrationCompleted());
        log.debug("extractor fragments were successfully added");
    }


    static RuleFragment createCopyFieldExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let %resultvar% = $message.${field};
                        set_field(
                          field: ${newField},
                          value: %resultvar%
                        );""".replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_field")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build(),
                                string("newField").description("New field to copy value to").build()
                        ))
                        .returnType(Object.class)
                        .description("Copy field value to a new field")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Extract value to new field")
                        .ruleBuilderTitle("Extract field '${field}' and set to new field '${newField}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.EXTRACTORS)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }


    static RuleFragment createRegexExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let regex_pattern = ${pattern};
                        let regex_results = regex(regex_pattern, to_string($message.${field}));
                        let %resultvar% = regex_results["0"];
                        set_field(${newField}, %resultvar%);"""
                        .replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_regex")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build(),
                                string("pattern").description("The regular expression used for extraction. First matcher group is used.").build(),
                                string("newField").description("New field to copy value to").build()
                        ))
                        .returnType(String.class)
                        .description("Copy extracted regular expression of field value to a new field")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Extract regular expression to new field")
                        .ruleBuilderTitle("Extract regular expression '${pattern}' for field '${field}' and set to new field '${newField}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.EXTRACTORS)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }

    private void addFragment(RuleFragment ruleFragment) {
        Optional<RuleFragment> existingFragment = ruleFragmentService.get(ruleFragment.getName());
        existingFragment.ifPresent(fragment -> ruleFragmentService.delete(fragment.getName()));
        ruleFragmentService.save(ruleFragment);
    }

    public record MigrationCompleted() {}

}
