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
import org.graylog.plugins.pipelineprocessor.functions.IsNotNull;
import org.graylog.plugins.pipelineprocessor.functions.IsNull;
import org.graylog.plugins.pipelineprocessor.functions.conversion.BooleanConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.DoubleConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsBoolean;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsCollection;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsDouble;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsList;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsLong;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsMap;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsNumber;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsString;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.MapConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.dates.IsDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.ParseDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.IsPeriod;
import org.graylog.plugins.pipelineprocessor.functions.ips.CidrMatch;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddress;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddressConversion;
import org.graylog.plugins.pipelineprocessor.functions.ips.IsIp;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.Contains;
import org.graylog.plugins.pipelineprocessor.functions.strings.EndsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.GrokMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.StartsWith;
import org.graylog.plugins.pipelineprocessor.functions.urls.IsUrl;
import org.graylog.plugins.pipelineprocessor.functions.urls.URL;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlConversion;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.BaseFragmentTest;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class V20230724092100_AddFieldConditionsTest extends BaseFragmentTest {

    V20230724092100_AddFieldConditions migration;
    @Mock
    private RuleFragmentService ruleFragmentService;

    @BeforeClass
    @SuppressForbidden("Allow using default thread factory")
    public static void initialize() {
        final Map<String, Function<?>> functions = commonFunctions();
        functions.put(SetField.NAME, new SetField());
        functions.put(IsBoolean.NAME, new IsBoolean());
        functions.put(BooleanConversion.NAME, new BooleanConversion());
        functions.put(IsCollection.NAME, new IsCollection());
        functions.put(IsDouble.NAME, new IsDouble());
        functions.put(IsDate.NAME, new IsDate());
        functions.put(DoubleConversion.NAME, new DoubleConversion());
        functions.put(ParseDate.NAME, new ParseDate());
        functions.put(IsList.NAME, new IsList());
        functions.put(IsIp.NAME, new IsIp());
        functions.put(IpAddressConversion.NAME, new IpAddressConversion());
        functions.put(IsLong.NAME, new IsLong());
        functions.put(LongConversion.NAME, new LongConversion());
        functions.put(IsNotNull.NAME, new IsNotNull());
        functions.put(IsNull.NAME, new IsNull());
        functions.put(IsMap.NAME, new IsMap());
        functions.put(MapConversion.NAME, new MapConversion());
        functions.put(IsNumber.NAME, new IsNumber());
        functions.put(IsPeriod.NAME, new IsPeriod());
        functions.put(IsString.NAME, new IsString());
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(IsUrl.NAME, new IsUrl());
        functions.put(UrlConversion.NAME, new UrlConversion());
        functions.put(CidrMatch.NAME, new CidrMatch());
        functions.put(Contains.NAME, new Contains());
        functions.put(StartsWith.NAME, new StartsWith());
        functions.put(EndsWith.NAME, new EndsWith());

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
        functionRegistry = new FunctionRegistry(functions);
    }

    @Before
    public void initializeMigration() {
        migration = new V20230724092100_AddFieldConditions(ruleFragmentService);
    }

    @Test
    public void testFieldBoolean() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        message.addField("bool", true);
        final RuleFragment fragment = migration.createCheckFieldType("bool");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "bool"));
        evaluateCondition(testRule, message, true);

        message.addField("boolfalse", false);
        testRule = createFragmentSource(fragment, Map.of("field", "boolfalse"));
        evaluateCondition(testRule, message, true);

        message.addField("boolstring", "true");
        testRule = createFragmentSource(fragment, Map.of("field", "boolstring"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "boolstring", "attemptConversion", true));
        evaluateCondition(testRule, message, true);

        message.addField("nobool", "fase");
        testRule = createFragmentSource(fragment, Map.of("field", "nobool"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldCollection() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("collection");

        message.addField("list", List.of("a", "b", "c"));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "list"));
        evaluateCondition(testRule, message, true);

        message.addField("nocollection", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "nocollection"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldDouble() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("double");

        message.addField("double", 2.25);
        Rule testRule = createFragmentSource(fragment, Map.of("field", "double"));
        evaluateCondition(testRule, message, true);

        message.addField("doublestring", "2.25");
        testRule = createFragmentSource(fragment, Map.of("field", "doublestring"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "doublestring", "attemptConversion", true));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldDate() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckDateField();

        message.addField("date", Tools.nowUTC());
        Rule testRule = createFragmentSource(fragment, Map.of("field", "date"));
        evaluateCondition(testRule, message, true);

        message.addField("datestring", "01 07 2023");
        testRule = createFragmentSource(fragment, Map.of("field", "datestring", "pattern", "d M y"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldList() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("list");

        message.addField("list", List.of("a", "b", "c"));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "list"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldIp() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("ip");

        message.addField("ip", new IpAddress(InetAddress.getLoopbackAddress()));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "ip"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "127.0.0.1");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "attemptConversion", true));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldLong() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("long");

        message.addField("long", 1234L);
        Rule testRule = createFragmentSource(fragment, Map.of("field", "long"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "1234");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "attemptConversion", true));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldMap() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("map");

        message.addField("map", Map.of("a", "b"));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "map"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldNotNull() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("not_null");

        message.addField("notnull", "anything");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "notnull"));
        evaluateCondition(testRule, message, true);

        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldNull() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("null");

        Rule testRule = createFragmentSource(fragment, Map.of("field", "null"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldNumber() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("number");

        message.addField("number", 1234);
        Rule testRule = createFragmentSource(fragment, Map.of("field", "number"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldString() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("string");

        message.addField("string", "iamastring");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, true);

        message.addField("notastring", 1);
        testRule = createFragmentSource(fragment, Map.of("field", "notastring"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldPeriod() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldTypeNoConversion("period");

        message.addField("period", Period.parse("P1M"));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "period"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldUrl() throws MalformedURLException {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCheckFieldType("url");

        message.addField("url", new URL("http://dummy.net"));
        Rule testRule = createFragmentSource(fragment, Map.of("field", "url"));
        evaluateCondition(testRule, message, true);

        message.addField("string", "http://dummy.net");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "attemptConversion", true));
        evaluateCondition(testRule, message, true);

        message.addField("string", "iamastring");
        testRule = createFragmentSource(fragment, Map.of("field", "string"));
        evaluateCondition(testRule, message, false);
    }

    @Test
    public void testFieldCidr() throws MalformedURLException {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createCIDRMatchField();

        message.addField("ip", "192.168.1.42");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "ip", "cidr", "192.168.1.15/24"));
        evaluateCondition(testRule, message, true);

        message.addField("noip", "somestring");
        testRule = createFragmentSource(fragment, Map.of("field", "noip", "cidr", "192.168.1.15/24"));
        evaluateCondition(testRule, message, false);

    }

    @Test
    public void testFieldContains() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createStringContainsField();

        message.addField("string", "snickerdoodledoo");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "doodle"));
        evaluateCondition(testRule, message, true);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "noodle"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "DOODLE"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "DOODLE", "ignoreCase", true));
        evaluateCondition(testRule, message, true);
    }

    @Test
    public void testFieldStartsWith() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createStringStartsWithField();

        message.addField("string", "snickerdoodledoo");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "snicker"));
        evaluateCondition(testRule, message, true);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "doodle"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "SNICKER"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "SNICKER", "ignoreCase", true));
        evaluateCondition(testRule, message, true);
    }

    @Test
    public void testFieldEndsWith() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createStringEndsWithField();

        message.addField("string", "snickerdoodledoo");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "doo"));
        evaluateCondition(testRule, message, true);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "doodle"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "DOO"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "string", "search", "DOO", "ignoreCase", true));
        evaluateCondition(testRule, message, true);
    }

    @Test
    public void testFieldGrokMatches() {
        Message message = new Message("Dummy Message", "test", Tools.nowUTC());
        final RuleFragment fragment = migration.createGrokMatchesField();

        message.addField("string", "string");
        message.addField("number", "10");
        Rule testRule = createFragmentSource(fragment, Map.of("field", "string", "pattern", "%{BASE10NUM}"));
        evaluateCondition(testRule, message, false);
        testRule = createFragmentSource(fragment, Map.of("field", "number", "pattern", "%{BASE10NUM}"));
        evaluateCondition(testRule, message, true);
    }

}
