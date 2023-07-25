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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20230724092100_AddFieldConditions extends Migration {

    private static final Logger log = LoggerFactory.getLogger(V20230613154400_AddImplicitToStringFragments.class);
    private final RuleFragmentService ruleFragmentService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230724092100_AddFieldConditions(RuleFragmentService ruleFragmentService, ClusterConfigService clusterConfigService) {
        this.ruleFragmentService = ruleFragmentService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-07-24T09:21:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding field condition fragments via migration");
        if (Objects.nonNull(clusterConfigService.get(MigrationCompleted.class))) {
            log.debug("Migration already completed!");
//            return;
        }
        String[] noConversionTypes = {"collection", "ip", "list", "not_null", "null", "number", "period"};
        Arrays.stream(noConversionTypes).forEach(type -> addFragment(createCheckFieldTypeNoConversion(type)));
        String[] conversionTypes = {"bool", "double", "long", "map", "string", "url"};
        Arrays.stream(conversionTypes).forEach(type -> addFragment(createCheckFieldType(type)));
        addFragment(createCheckDateField());

        clusterConfigService.write(new MigrationCompleted());
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
                        .ruleBuilderName("Check if field is " + type)
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
                        .ruleBuilderName("Check if field is " + type)
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
                        .ruleBuilderName("Check if field is date")
                        .ruleBuilderTitle("Check if value in '${field}' is a date")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                        .build())
                .isCondition()
                .build();
    }


    private void addFragment(RuleFragment ruleFragment) {
        Optional<RuleFragment> existingFragment = ruleFragmentService.get(ruleFragment.getName());
        existingFragment.ifPresent(fragment -> ruleFragmentService.delete(fragment.getName()));
        ruleFragmentService.save(ruleFragment);
    }

    public record MigrationCompleted() {}

}
