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

import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupValue;
import org.graylog.plugins.pipelineprocessor.functions.messages.GetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexReplace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Split;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.BaseFragmentTest;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V20230720161500_AddExtractorFragmentsTest extends BaseFragmentTest {


    private static LookupTableService lookupTableService;
    private static LookupTableService.Function lookupServiceFunction;
    private static LookupTable lookupTable;
    private final MessageFactory messageFactory = new TestMessageFactory();

    @BeforeAll
    public static void initialize() {
        final Map<String, Function<?>> functions = commonFunctions();
        functions.put(GetField.NAME, new GetField());
        functions.put(SetField.NAME, new SetField());
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(RegexMatch.NAME, new RegexMatch());
        functions.put(RegexReplace.NAME, new RegexReplace());
        functions.put(Split.NAME, new Split());

        lookupTable = mock(LookupTable.class);
        when(lookupTable.lookup("ExistingKey")).thenReturn(LookupResult.builder()
                .single("ThisKeysValue")
                .cacheTTL(1000)
                .build());
        lookupTableService = mock(LookupTableService.class, RETURNS_DEEP_STUBS);
        when(lookupTableService.getTable(anyString())).thenReturn(lookupTable);
        lookupServiceFunction = new LookupTableService.Function(lookupTableService, "lookup-table");
        when(lookupTableService.newBuilder().lookupTable(anyString()).build()).thenReturn(lookupServiceFunction);
        functions.put(LookupValue.NAME, new LookupValue(lookupTableService));

        functionRegistry = new FunctionRegistry(functions);
    }


    @Test
    void testCopyField() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createCopyFieldExtractor();
        Map<String, Object> params = Map.of("field", "message", "newField", "copyfield");
        Rule rule = super.createFragmentSource(fragment, params);
        final Message message = evaluateRule(rule, messageFactory.createMessage("Dummy Message", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("Dummy Message");
    }

    @Test
    void testRegex() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createRegexExtractor();
        Map<String, Object> params = Map.of("field", "message", "pattern", "^.*(doo...).*$", "newField", "copyfield");
        Rule rule = super.createFragmentSource(fragment, params);
        final Message message = evaluateRule(rule, messageFactory.createMessage("bippitysnickerdoodledoobadoo", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("doobad");
    }

    @Test
    void testRegexReplacement() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createRegexReplacementExtractor();
        Map<String, Object> params = Map.of(
                "field", "message",
                "pattern", "dog",
                "replacement", "cat",
                "newField", "copyfield"
        );
        Rule rule = super.createFragmentSource(fragment, params);
        Message message = evaluateRule(rule, messageFactory.createMessage("zzzdogzzzdogzzz", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("zzzcatzzzcatzzz");
        params = Map.of(
                "field", "message",
                "pattern", "dog",
                "replacement", "cat",
                "newField", "copyfield2",
                "replaceAll", false
        );
        rule = super.createFragmentSource(fragment, params);
        message = evaluateRule(rule, messageFactory.createMessage("zzzdogzzzdogzzz", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield2")).isEqualTo("zzzcatzzzdogzzz");

    }

    @Test
    void testSplitIndex() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createSplitIndexExtractor();
        Map<String, Object> params = Map.of(
                "field", "message",
                "character", ",",
                "targetIndex", 1,
                "newField", "copyfield"
        );
        Rule rule = super.createFragmentSource(fragment, params);
        Message message = evaluateRule(rule, messageFactory.createMessage("cat,dog,mouse", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("dog");
    }

    @Test
    void testLookup() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createLookupExtractor();
        Map<String, Object> params = Map.of(
                "field", "message",
                "lookupTable", "lookup-table",
                "newField", "copyfield"
        );
        Rule rule = super.createFragmentSource(fragment, params);
        Message message = evaluateRule(rule, messageFactory.createMessage("ExistingKey", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("ThisKeysValue");

        message = evaluateRule(rule, messageFactory.createMessage("NoKey", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isNull();
    }


}
