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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class V20230613154400_AddImplicitToStringFragments extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20230613154400_AddImplicitToStringFragments.class);
    private final RuleFragmentService ruleFragmentService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230613154400_AddImplicitToStringFragments(RuleFragmentService ruleFragmentService, ClusterConfigService clusterConfigService) {
        this.ruleFragmentService = ruleFragmentService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-06-13T15:44:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding implicit to_string fragments via migration");
        if (Objects.nonNull(clusterConfigService.get(MigrationCompleted.class))) {
            log.debug("Migration already completed!");
//            return;
        }

        addFragment(createSubstringFragment());
        addFragment(createDateFragment());

        clusterConfigService.write(new MigrationCompleted());
        log.debug("implicit to_string fragments were successfully added");
    }

    private static RuleFragment createSubstringFragment() {
        return RuleFragment.builder()
                .fragment("""
                        let gl2_fragment_substring_results = substring(
                            value: to_string(${value}),
                            start: ${start}<#if end??>,
                            indexEnd: ${end}</#if>
                          );""")
                .descriptor(FunctionDescriptor.builder()
                        .name("get_substring")
                        .params(ImmutableList.of(
                                object("value").primary().description("The string to extract from").primary().build(),
                                integer("start").allowNegatives(true).description("The position to start from, negative means count back from the end of the String by this many characters").build(),
                                integer("end").optional().allowNegatives(true).description("The position to end at (exclusive), negative means count back from the end of the String by this many characters, defaults to length of the input string").build()
                        ))
                        .returnType(String.class)
                        .description("Get substring of value")
                        .ruleBuilderEnabled()
                        .ruleBuilderTitle("Get substring from '${start}' to '${end!\"end\"}' of value")
                        .build())
                .fragmentOutputVariable("gl2_fragment_substring_results")
                .build();
    }

    private static RuleFragment createDateFragment() {
        return RuleFragment.builder()
                .fragment("""
                        let gl2_fragment_date_results = parse_date(
                          value: to_string(${value}),
                          pattern: ${pattern}<#if locale??>,
                          locale: ${locale}</#if><#if timezone??>,
                          locale: ${timezone}</#if>
                        );""")
                .descriptor(FunctionDescriptor.builder()
                        .name("get_date")
                        .params(ImmutableList.of(
                                object("value").primary().description("Date string to parse").build(),
                                string("pattern").description("The pattern to parse the date with, see http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html").build(),
                                string("locale").optional().description("The locale to parse the date with, see https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html").build(),
                                string("timezone").optional().description("The timezone to apply to the date, defaults to UTC").build()
                        ))
                        .returnType(DateTime.class)
                        .description("Parses a value using the given date format")
                        .ruleBuilderEnabled()
                        .ruleBuilderTitle("Parse date from value")
                        .build())
                .fragmentOutputVariable("gl2_fragment_date_results")
                .build();
    }

    private void addFragment(RuleFragment ruleFragment) {
        Optional<RuleFragment> existingFragment = ruleFragmentService.get(ruleFragment.getName());
        existingFragment.ifPresent(fragment -> ruleFragmentService.delete(fragment.getName()));
        ruleFragmentService.save(ruleFragment);
    }

    public record MigrationCompleted() {}
}
