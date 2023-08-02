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
import org.graylog.plugins.pipelineprocessor.functions.messages.GetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexReplace;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.BaseFragmentTest;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class V20230720161500_AddExtractorFragmentsTest extends BaseFragmentTest {

    V20230720161500_AddExtractorFragments migration;

    @BeforeClass
    public static void initialize() {
        final Map<String, Function<?>> functions = commonFunctions();
        functions.put(GetField.NAME, new GetField());
        functions.put(SetField.NAME, new SetField());
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(RegexMatch.NAME, new RegexMatch());
        functions.put(RegexReplace.NAME, new RegexReplace());
        functionRegistry = new FunctionRegistry(functions);
    }


    @Test
    public void testCopyField() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createCopyFieldExtractor();
        Map<String, Object> params = Map.of("field", "message", "newField", "copyfield");
        Rule rule = super.createFragmentSource(fragment, params);
        final Message message = evaluateRule(rule, new Message("Dummy Message", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("Dummy Message");
    }

    @Test
    public void testRegex() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createRegexExtractor();
        Map<String, Object> params = Map.of("field", "message", "pattern", "^.*(doo...).*$", "newField", "copyfield");
        Rule rule = super.createFragmentSource(fragment, params);
        final Message message = evaluateRule(rule, new Message("bippitysnickerdoodledoobadoo", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("doobad");
    }

    @Test
    public void testRegexReplacement() {
        RuleFragment fragment = V20230720161500_AddExtractorFragments.createRegexReplacementExtractor();
        Map<String, Object> params = Map.of(
                "field", "message",
                "pattern", "dog",
                "replacement", "cat",
                "newField", "copyfield"
        );
        Rule rule = super.createFragmentSource(fragment, params);
        Message message = evaluateRule(rule, new Message("zzzdogzzzdogzzz", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield")).isEqualTo("zzzcatzzzcatzzz");
        params = Map.of(
                "field", "message",
                "pattern", "dog",
                "replacement", "cat",
                "newField", "copyfield2",
                "replaceAll", false
        );
        rule = super.createFragmentSource(fragment, params);
        message = evaluateRule(rule, new Message("zzzdogzzzdogzzz", "test", Tools.nowUTC()));
        assertThat(message.getField("copyfield2")).isEqualTo("zzzcatzzzdogzzz");

    }


}
