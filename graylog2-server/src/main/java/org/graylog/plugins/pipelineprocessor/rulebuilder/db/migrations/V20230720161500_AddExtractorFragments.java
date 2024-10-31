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

public class V20230720161500_AddExtractorFragments extends Migration {

    private static final Logger log = LoggerFactory.getLogger(V20230720161500_AddExtractorFragments.class);
    private final RuleFragmentService ruleFragmentService;

    @Inject
    public V20230720161500_AddExtractorFragments(RuleFragmentService ruleFragmentService) {
        this.ruleFragmentService = ruleFragmentService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-06-13T15:44:00Z");
    }

    @Override
    public void upgrade() {
        log.debug("Adding extractor fragments via migration");

        ruleFragmentService.upsert(createCopyFieldExtractor());
        ruleFragmentService.upsert(createRegexExtractor());
        ruleFragmentService.upsert(createRegexReplacementExtractor());
        ruleFragmentService.upsert(createJsonExtractor());
        ruleFragmentService.upsert(createSplitIndexExtractor());
        ruleFragmentService.upsert(createLookupExtractor());

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

    static RuleFragment createRegexReplacementExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let regex_pattern = ${pattern};
                        let %resultvar% = regex_replace(
                            pattern: regex_pattern,
                            value: to_string($message.${field}),
                            replacement: ${replacement}<#if replaceAll??>,
                            replace_all: ${replaceAll?c}</#if>
                        );
                        set_field(${newField}, %resultvar%);"""
                        .replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_regex_replace")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build(),
                                string("pattern").description("The regular expression used for extraction.").build(),
                                string("replacement").description("The replacement used for the matching text. Please refer to the Matcher API documentation for the possible options.").build(),
                                bool("replaceAll").description("Replace all occurences of the pattern, or only the first occurence. (default: true)").build(),
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

    static RuleFragment createJsonExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let %resultvar% = parse_json(to_string($message.${field}));
                        set_fields(to_map(%resultvar%));"""
                        .replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_json")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build()
                        ))
                        .returnType(String.class)
                        .description("Parse field as json and set to fields")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Extract json and set to fields")
                        .ruleBuilderTitle("Extract Json in field '${field}' and set to new fields")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.EXTRACTORS)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }

    static RuleFragment createSplitIndexExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let %resultvar% = split(${character}, to_string($message.${field}))[${targetIndex}];
                        set_field(${newField}, %resultvar%);"""
                        .replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_split_index")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build(),
                                string("character").description("What character to split on").build(),
                                integer("targetIndex").description("What part of the split string to use (0-based)").build(),
                                string("newField").description("New field to copy value to").build()
                        ))
                        .returnType(String.class)
                        .description("Split field into tokens by character and set one token to new field.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Extract split & index")
                        .ruleBuilderTitle("Extract token number '${targetIndex}' from split field '${field}' and set to new field '${newField}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.EXTRACTORS)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }

    static RuleFragment createLookupExtractor() {
        String resultvariable = "gl2_fragment_extractor_" + System.currentTimeMillis();
        return RuleFragment.builder()
                .fragment("""
                        let %resultvar% = lookup_value(${lookupTable}, to_string($message.${field}));
                        set_field(${newField}, %resultvar%);"""
                        .replace("%resultvar%", resultvariable))
                .descriptor(FunctionDescriptor.builder()
                        .name("extract_lookup")
                        .params(ImmutableList.of(
                                string("field").description("Field to extract").build(),
                                string("lookupTable").description("Lookup table to use").build(),
                                string("newField").description("New field to copy value to").build()
                        ))
                        .returnType(String.class)
                        .description("Lookup value for key in field in lookup table and set it to new field.")
                        .ruleBuilderEnabled()
                        .ruleBuilderName("Extract lookup value")
                        .ruleBuilderTitle("Extract value for field '${field}', do lookup in '${lookupTable}' and set value to new field '${newField}'")
                        .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.EXTRACTORS)
                        .build())
                .fragmentOutputVariable(resultvariable)
                .build();
    }


}
