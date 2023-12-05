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

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.GrokMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.BaseFragmentTest;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class V20220522125200_AddSetGrokToFieldsExtractorFragmentsTest extends BaseFragmentTest {

    V20220522125200_AddSetGrokToFieldsExtractorFragments migration;

    @BeforeClass
    @SuppressForbidden("Allow using default thread factory")
    public static void initialize() {
        final Map<String, Function<?>> functions = commonFunctions();
        final GrokPatternService grokPatternService = mock(GrokPatternService.class);
        Set<GrokPattern> patterns = Sets.newHashSet(
                GrokPattern.create("BASE10NUM", "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))")
        );
        when(grokPatternService.loadAll()).thenReturn(patterns);
        final EventBus clusterBus = new EventBus();
        final GrokPatternRegistry grokPatternRegistry = new GrokPatternRegistry(clusterBus,
                grokPatternService,
                Executors.newScheduledThreadPool(1));
        functions.put(GrokMatch.NAME, new GrokMatch(grokPatternRegistry));
        functions.put(SetFields.NAME, new SetFields());
        functions.put(StringConversion.NAME, new StringConversion());
        functionRegistry = new FunctionRegistry(functions);
    }


    @Test
    public void testGrokExtract() {
        final RuleFragment fragment = V20220522125200_AddSetGrokToFieldsExtractorFragments.createSetGrokToFieldsFragment();

        Rule testRule = createFragmentSource(fragment, Map.of(
                "field", "message",
                "grokPattern", "^%{BASE10NUM:number}\\\\s+"));
        Message result = evaluateRule(testRule, new Message("99 Problems", "test", Tools.nowUTC()));
        assertThat(result.getField("number")).isEqualTo("99");

        testRule = createFragmentSource(fragment, Map.of(
                "field", "message",
                "grokPattern", "^%{BASE10NUM}\\\\s+",
                "grokNamedOnly", true));
        final Message inMessage = new Message("99 Problems", "test", Tools.nowUTC());
        result = evaluateRule(testRule, inMessage);
        assertThat(result.getFieldCount()).isEqualTo(inMessage.getFieldCount());
    }


}
