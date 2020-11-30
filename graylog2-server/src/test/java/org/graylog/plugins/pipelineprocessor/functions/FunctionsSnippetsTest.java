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
package org.graylog.plugins.pipelineprocessor.functions;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.net.InetAddresses;
import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
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
import org.graylog.plugins.pipelineprocessor.functions.dates.DateConversion;
import org.graylog.plugins.pipelineprocessor.functions.dates.FlexParseDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.FormatDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.IsDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.Now;
import org.graylog.plugins.pipelineprocessor.functions.dates.ParseDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.ParseUnixMilliseconds;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Days;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Hours;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.IsPeriod;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Millis;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Minutes;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Months;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.PeriodParseFunction;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Seconds;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Weeks;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Years;
import org.graylog.plugins.pipelineprocessor.functions.debug.MetricCounterIncrement;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base16Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base16Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32HumanDecode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32HumanEncode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64UrlDecode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64UrlEncode;
import org.graylog.plugins.pipelineprocessor.functions.hashing.CRC32;
import org.graylog.plugins.pipelineprocessor.functions.hashing.CRC32C;
import org.graylog.plugins.pipelineprocessor.functions.hashing.MD5;
import org.graylog.plugins.pipelineprocessor.functions.hashing.Murmur3_128;
import org.graylog.plugins.pipelineprocessor.functions.hashing.Murmur3_32;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA1;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA256;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA512;
import org.graylog.plugins.pipelineprocessor.functions.ips.CidrMatch;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddress;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddressConversion;
import org.graylog.plugins.pipelineprocessor.functions.ips.IsIp;
import org.graylog.plugins.pipelineprocessor.functions.json.IsJson;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonParse;
import org.graylog.plugins.pipelineprocessor.functions.json.SelectJsonPath;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupAddStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupClearKey;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupRemoveStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupSetStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupSetValue;
import org.graylog.plugins.pipelineprocessor.functions.messages.CloneMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.DropMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveFromStream;
import org.graylog.plugins.pipelineprocessor.functions.messages.RenameField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RouteToStream;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog.plugins.pipelineprocessor.functions.messages.TrafficAccountingSize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Abbreviate;
import org.graylog.plugins.pipelineprocessor.functions.strings.Capitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Concat;
import org.graylog.plugins.pipelineprocessor.functions.strings.Contains;
import org.graylog.plugins.pipelineprocessor.functions.strings.EndsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.FirstNonNull;
import org.graylog.plugins.pipelineprocessor.functions.strings.GrokMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.Join;
import org.graylog.plugins.pipelineprocessor.functions.strings.KeyValue;
import org.graylog.plugins.pipelineprocessor.functions.strings.Length;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexReplace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Replace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Split;
import org.graylog.plugins.pipelineprocessor.functions.strings.StartsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.Substring;
import org.graylog.plugins.pipelineprocessor.functions.strings.Swapcase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uncapitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uppercase;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogFacilityConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogLevelConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogPriorityConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogPriorityToStringConversion;
import org.graylog.plugins.pipelineprocessor.functions.urls.IsUrl;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlConversion;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlDecode;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlEncode;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FunctionsSnippetsTest extends BaseParserTest {
    @org.junit.Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    public static final DateTime GRAYLOG_EPOCH = DateTime.parse("2010-07-30T16:03:25Z");
    private static final EventBus eventBus = new EventBus();
    private static StreamCacheService streamCacheService;
    private static Stream otherStream;
    private static MetricRegistry metricRegistry = new MetricRegistry();

    private static LookupTableService lookupTableService;
    private static LookupTableService.Function lookupServiceFunction;
    private static LookupTable lookupTable;

    @BeforeClass
    @SuppressForbidden("Allow using default thread factory")
    public static void registerFunctions() {
        final Map<String, Function<?>> functions = commonFunctions();

        functions.put(BooleanConversion.NAME, new BooleanConversion());
        functions.put(DoubleConversion.NAME, new DoubleConversion());
        functions.put(LongConversion.NAME, new LongConversion());
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(MapConversion.NAME, new MapConversion());

        // message related functions
        functions.put(HasField.NAME, new HasField());
        functions.put(SetField.NAME, new SetField());
        functions.put(SetFields.NAME, new SetFields());
        functions.put(RenameField.NAME, new RenameField());
        functions.put(RemoveField.NAME, new RemoveField());

        functions.put(DropMessage.NAME, new DropMessage());
        functions.put(CreateMessage.NAME, new CreateMessage());
        functions.put(CloneMessage.NAME, new CloneMessage());
        functions.put(TrafficAccountingSize.NAME, new TrafficAccountingSize());

        // route to stream mocks
        final StreamService streamService = mock(StreamService.class);

        otherStream = mock(Stream.class, "some stream id2");
        when(otherStream.isPaused()).thenReturn(false);
        when(otherStream.getTitle()).thenReturn("some name");
        when(otherStream.getId()).thenReturn("id2");

        when(streamService.loadAll()).thenReturn(Lists.newArrayList(defaultStream, otherStream));
        when(streamService.loadAllEnabled()).thenReturn(Lists.newArrayList(defaultStream, otherStream));
        streamCacheService = new StreamCacheService(eventBus, streamService, null);
        streamCacheService.startAsync().awaitRunning();
        final Provider<Stream> defaultStreamProvider = () -> defaultStream;
        functions.put(RouteToStream.NAME, new RouteToStream(streamCacheService, defaultStreamProvider));
        functions.put(RemoveFromStream.NAME, new RemoveFromStream(streamCacheService, defaultStreamProvider));

        lookupTableService = mock(LookupTableService.class, RETURNS_DEEP_STUBS);
        lookupTable = spy(LookupTable.class);
        when(lookupTableService.getTable(anyString())).thenReturn(lookupTable);
        lookupServiceFunction = new LookupTableService.Function(lookupTableService, "table");
        when(lookupTableService.newBuilder().lookupTable(anyString()).build()).thenReturn(lookupServiceFunction);

        // input related functions
        // TODO needs mock
        //functions.put(FromInput.NAME, new FromInput());

        // generic functions
        functions.put(RegexMatch.NAME, new RegexMatch());
        functions.put(RegexReplace.NAME, new RegexReplace());

        // string functions
        functions.put(Abbreviate.NAME, new Abbreviate());
        functions.put(Capitalize.NAME, new Capitalize());
        functions.put(Concat.NAME, new Concat());
        functions.put(Contains.NAME, new Contains());
        functions.put(EndsWith.NAME, new EndsWith());
        functions.put(Lowercase.NAME, new Lowercase());
        functions.put(Substring.NAME, new Substring());
        functions.put(Swapcase.NAME, new Swapcase());
        functions.put(Uncapitalize.NAME, new Uncapitalize());
        functions.put(Uppercase.NAME, new Uppercase());
        functions.put(KeyValue.NAME, new KeyValue());
        functions.put(Join.NAME, new Join());
        functions.put(Split.NAME, new Split());
        functions.put(StartsWith.NAME, new StartsWith());
        functions.put(Replace.NAME, new Replace());
        functions.put(Length.NAME, new Length());
        functions.put(FirstNonNull.NAME, new FirstNonNull());

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        functions.put(JsonParse.NAME, new JsonParse(objectMapper));
        functions.put(SelectJsonPath.NAME, new SelectJsonPath(objectMapper));

        functions.put(DateConversion.NAME, new DateConversion());
        functions.put(Now.NAME, new Now());
        functions.put(FlexParseDate.NAME, new FlexParseDate());
        functions.put(ParseDate.NAME, new ParseDate());
        functions.put(ParseUnixMilliseconds.NAME, new ParseUnixMilliseconds());
        functions.put(FormatDate.NAME, new FormatDate());

        functions.put(Years.NAME, new Years());
        functions.put(Months.NAME, new Months());
        functions.put(Weeks.NAME, new Weeks());
        functions.put(Days.NAME, new Days());
        functions.put(Hours.NAME, new Hours());
        functions.put(Minutes.NAME, new Minutes());
        functions.put(Seconds.NAME, new Seconds());
        functions.put(Millis.NAME, new Millis());
        functions.put(PeriodParseFunction.NAME, new PeriodParseFunction());

        functions.put(CRC32.NAME, new CRC32());
        functions.put(CRC32C.NAME, new CRC32C());
        functions.put(MD5.NAME, new MD5());
        functions.put(Murmur3_32.NAME, new Murmur3_32());
        functions.put(Murmur3_128.NAME, new Murmur3_128());
        functions.put(SHA1.NAME, new SHA1());
        functions.put(SHA256.NAME, new SHA256());
        functions.put(SHA512.NAME, new SHA512());

        functions.put(Base16Encode.NAME, new Base16Encode());
        functions.put(Base16Decode.NAME, new Base16Decode());
        functions.put(Base32Encode.NAME, new Base32Encode());
        functions.put(Base32Decode.NAME, new Base32Decode());
        functions.put(Base32HumanEncode.NAME, new Base32HumanEncode());
        functions.put(Base32HumanDecode.NAME, new Base32HumanDecode());
        functions.put(Base64Encode.NAME, new Base64Encode());
        functions.put(Base64Decode.NAME, new Base64Decode());
        functions.put(Base64UrlEncode.NAME, new Base64UrlEncode());
        functions.put(Base64UrlDecode.NAME, new Base64UrlDecode());

        functions.put(IpAddressConversion.NAME, new IpAddressConversion());
        functions.put(CidrMatch.NAME, new CidrMatch());

        functions.put(IsNull.NAME, new IsNull());
        functions.put(IsNotNull.NAME, new IsNotNull());

        functions.put(SyslogPriorityConversion.NAME, new SyslogPriorityConversion());
        functions.put(SyslogPriorityToStringConversion.NAME, new SyslogPriorityToStringConversion());
        functions.put(SyslogFacilityConversion.NAME, new SyslogFacilityConversion());
        functions.put(SyslogLevelConversion.NAME, new SyslogLevelConversion());

        functions.put(UrlConversion.NAME, new UrlConversion());
        functions.put(UrlDecode.NAME, new UrlDecode());
        functions.put(UrlEncode.NAME, new UrlEncode());

        functions.put(IsBoolean.NAME, new IsBoolean());
        functions.put(IsNumber.NAME, new IsNumber());
        functions.put(IsDouble.NAME, new IsDouble());
        functions.put(IsLong.NAME, new IsLong());
        functions.put(IsString.NAME, new IsString());
        functions.put(IsCollection.NAME, new IsCollection());
        functions.put(IsList.NAME, new IsList());
        functions.put(IsMap.NAME, new IsMap());
        functions.put(IsDate.NAME, new IsDate());
        functions.put(IsPeriod.NAME, new IsPeriod());
        functions.put(IsIp.NAME, new IsIp());
        functions.put(IsJson.NAME, new IsJson());
        functions.put(IsUrl.NAME, new IsUrl());

        final GrokPatternService grokPatternService = mock(GrokPatternService.class);
        final GrokPattern greedyPattern = GrokPattern.create("GREEDY", ".*");
        Set<GrokPattern> patterns = Sets.newHashSet(
                greedyPattern,
                GrokPattern.create("GREEDY", ".*"),
                GrokPattern.create("BASE10NUM", "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))"),
                GrokPattern.create("NUMBER", "(?:%{BASE10NUM:UNWANTED})"),
                GrokPattern.create("UNDERSCORE", "(?<test_field>test)"),
                GrokPattern.create("NUM", "%{BASE10NUM}")
        );
        when(grokPatternService.loadAll()).thenReturn(patterns);
        when(grokPatternService.loadByName("GREEDY")).thenReturn(Optional.of(greedyPattern));
        final EventBus clusterBus = new EventBus();
        final GrokPatternRegistry grokPatternRegistry = new GrokPatternRegistry(clusterBus,
                                                                                grokPatternService,
                                                                                Executors.newScheduledThreadPool(1));
        functions.put(GrokMatch.NAME, new GrokMatch(grokPatternRegistry));
        functions.put(GrokExists.NAME, new GrokExists(grokPatternRegistry));

        functions.put(MetricCounterIncrement.NAME, new MetricCounterIncrement(metricRegistry));

        functions.put(LookupSetValue.NAME, new LookupSetValue(lookupTableService));
        functions.put(LookupClearKey.NAME, new LookupClearKey(lookupTableService));
        functions.put(LookupSetStringList.NAME, new LookupSetStringList(lookupTableService));
        functions.put(LookupAddStringList.NAME, new LookupAddStringList(lookupTableService));
        functions.put(LookupRemoveStringList.NAME, new LookupRemoveStringList(lookupTableService));

        functionRegistry = new FunctionRegistry(functions);
    }

    @Test
    public void stringConcat(){
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule, new Message("Dummy Message", "test", Tools.nowUTC()));

        assertThat(message.hasField("result")).isTrue();
        assertThat(message.getField("result")).isEqualTo("aabbcc");
    }

    @Test
    public void jsonpath() {
        final String json = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Evelyn Waugh\",\n" +
                "                \"title\": \"Sword of Honour\",\n" +
                "                \"price\": 12.99\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Herman Melville\",\n" +
                "                \"title\": \"Moby Dick\",\n" +
                "                \"isbn\": \"0-553-21311-3\",\n" +
                "                \"price\": 8.99\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"J. R. R. Tolkien\",\n" +
                "                \"title\": \"The Lord of the Rings\",\n" +
                "                \"isbn\": \"0-395-19395-8\",\n" +
                "                \"price\": 22.99\n" +
                "            }\n" +
                "        ],\n" +
                "        \"bicycle\": {\n" +
                "            \"color\": \"red\",\n" +
                "            \"price\": 19.95\n" +
                "        }\n" +
                "    },\n" +
                "    \"expensive\": 10\n" +
                "}";

        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule, new Message(json, "test", Tools.nowUTC()));

        assertThat(message.hasField("author_first")).isTrue();
        assertThat(message.getField("author_first")).isEqualTo("Nigel Rees");
        assertThat(message.hasField("author_last")).isTrue();
        assertThat(message.hasField("this_should_exist")).isTrue();
    }

    @Test
    public void json() {
        final String flatJson = "{\"str\":\"foobar\",\"int\":42,\"float\":2.5,\"bool\":true,\"array\":[1,2,3]}";
        final String nestedJson = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": {\n" +
                "            \"category\": \"reference\",\n" +
                "            \"author\": \"Nigel Rees\",\n" +
                "            \"title\": \"Sayings of the Century\",\n" +
                "            \"price\": 8.95\n" +
                "        },\n" +
                "        \"bicycle\": {\n" +
                "            \"color\": \"red\",\n" +
                "            \"price\": 19.95\n" +
                "        }\n" +
                "    },\n" +
                "    \"expensive\": 10\n" +
                "}";

        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = new Message("JSON", "test", Tools.nowUTC());
        message.addField("flat_json", flatJson);
        message.addField("nested_json", nestedJson);
        final Message evaluatedMessage = evaluateRule(rule, message);

        assertThat(evaluatedMessage.getField("message")).isEqualTo("JSON");
        assertThat(evaluatedMessage.getField("flat_json")).isEqualTo(flatJson);
        assertThat(evaluatedMessage.getField("nested_json")).isEqualTo(nestedJson);
        assertThat(evaluatedMessage.getField("str")).isEqualTo("foobar");
        assertThat(evaluatedMessage.getField("int")).isEqualTo(42);
        assertThat(evaluatedMessage.getField("float")).isEqualTo(2.5);
        assertThat(evaluatedMessage.getField("bool")).isEqualTo(true);
        assertThat(evaluatedMessage.getField("array")).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(evaluatedMessage.getField("store")).isInstanceOf(Map.class);
        assertThat(evaluatedMessage.getField("expensive")).isEqualTo(10);
    }

    @Test
    public void substring() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void dates() {
        final InstantMillisProvider clock = new InstantMillisProvider(GRAYLOG_EPOCH);
        DateTimeUtils.setCurrentMillisProvider(clock);

        try {
            final Rule rule;
            try {
                rule = parser.parseRule(ruleForTest(), false);
            } catch (ParseException e) {
                fail("Should not fail to parse", e);
                return;
            }
            final Message message = evaluateRule(rule);

            assertThat(actionsTriggered.get()).isTrue();
            assertThat(message).isNotNull();
            assertThat(message).isNotEmpty();
            assertThat(message.hasField("year")).isTrue();
            assertThat(message.getField("year")).isEqualTo(2010);
            assertThat(message.getField("timezone")).isEqualTo("UTC");

            // Date parsing locales
            assertThat(message.getField("german_year")).isEqualTo(1983);
            assertThat(message.getField("german_month")).isEqualTo(7);
            assertThat(message.getField("german_day")).isEqualTo(24);
            assertThat(message.getField("german_weekday")).isEqualTo(7);
            assertThat(message.getField("english_year")).isEqualTo(1983);
            assertThat(message.getField("english_month")).isEqualTo(7);
            assertThat(message.getField("english_day")).isEqualTo(24);
            assertThat(message.getField("french_year")).isEqualTo(1983);
            assertThat(message.getField("french_month")).isEqualTo(7);
            assertThat(message.getField("french_day")).isEqualTo(24);

            assertThat(message.getField("ts_hour")).isEqualTo(16);
            assertThat(message.getField("ts_minute")).isEqualTo(3);
            assertThat(message.getField("ts_second")).isEqualTo(25);
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    public void datesUnixTimestamps() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void digests() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void grok_exists() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void grok_exists_not() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isFalse();
    }

    @Test
    public void encodings() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void regexMatch() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);
        assertNotNull(message);
        assertTrue(message.hasField("matched_regex"));
        assertTrue(message.hasField("group_1"));
        assertThat((String) message.getField("named_group")).isEqualTo("cd.e");
    }

    @Test
    public void regexReplace() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        evaluateRule(rule);
        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void strings() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
        assertThat(message).isNotNull();
        assertThat(message.getField("has_xyz")).isInstanceOf(Boolean.class);
        assertThat((boolean) message.getField("has_xyz")).isFalse();
        assertThat(message.getField("string_literal")).isInstanceOf(String.class);
        assertThat((String) message.getField("string_literal")).isEqualTo("abcd\\.e\tfg\u03a9\363");
    }

    @Test
    public void stringLength() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(message).isNotNull();
        assertThat(message.getField("chars_utf8")).isEqualTo(5L);
        assertThat(message.getField("bytes_utf8")).isEqualTo(6L);
        assertThat(message.getField("chars_ascii")).isEqualTo(5L);
        assertThat(message.getField("bytes_ascii")).isEqualTo(5L);
    }

    @Test
    public void split() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
        assertThat(message).isNotNull();
        assertThat(message.getField("limit_0"))
                .asList()
                .isNotEmpty()
                .containsExactly("foo", "bar", "baz");
        assertThat(message.getField("limit_1"))
                .asList()
                .isNotEmpty()
                .containsExactly("foo:bar:baz");
        assertThat(message.getField("limit_2"))
                .asList()
                .isNotEmpty()
                .containsExactly("foo", "bar|baz");
    }

    @Test
    public void ipMatching() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message in = new Message("test", "test", Tools.nowUTC());
        in.addField("ip", "192.168.1.20");
        final Message message = evaluateRule(rule, in);

        assertThat(actionsTriggered.get()).isTrue();
        assertThat(message).isNotNull();
        assertThat(message.getField("ip_anon")).isEqualTo("192.168.1.0");
        assertThat(message.getField("ipv6_anon")).isEqualTo("2001:db8::");
    }

    @Test
    public void evalErrorSuppressed() {
        final Rule rule = parser.parseRule(ruleForTest(), false);

        final Message message = new Message("test", "test", Tools.nowUTC());
        message.addField("this_field_was_set", true);
        final EvaluationContext context = contextForRuleEval(rule, message);

        assertThat(context).isNotNull();
        assertThat(context.hasEvaluationErrors()).isFalse();
        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void newlyCreatedMessage() {
        final Message message = new Message("test", "test", Tools.nowUTC());
        message.addField("foo", "bar");
        message.addStream(mock(Stream.class));
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final EvaluationContext context = contextForRuleEval(rule, message);

        final Message origMessage = context.currentMessage();
        final Message newMessage = Iterables.getOnlyElement(context.createdMessages());

        assertThat(origMessage).isNotSameAs(newMessage);
        assertThat(newMessage.getMessage()).isEqualTo("new");
        assertThat(newMessage.getSource()).isEqualTo("synthetic");
        assertThat(newMessage.getStreams()).isEmpty();
        assertThat(newMessage.hasField("removed_again")).isFalse();
        assertThat(newMessage.getFieldAs(Boolean.class, "has_source")).isTrue();
        assertThat(newMessage.getFieldAs(String.class, "only_in")).isEqualTo("new message");
        assertThat(newMessage.getFieldAs(String.class, "multi")).isEqualTo("new message");
        assertThat(newMessage.getFieldAs(String.class, "foo")).isNull();
    }

    @Test
    public void clonedMessage() {
        final Message message = new Message("test", "test", Tools.nowUTC());
        message.addField("foo", "bar");
        message.addStream(mock(Stream.class));
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final EvaluationContext context = contextForRuleEval(rule, message);

        final Message origMessage = context.currentMessage();
        final Message clonedMessage = Iterables.get(context.createdMessages(), 0);
        final Message otherMessage = Iterables.get(context.createdMessages(), 1);

        assertThat(origMessage).isNotSameAs(clonedMessage);
        assertThat(clonedMessage).isNotNull();
        assertThat(clonedMessage.getMessage()).isEqualTo(origMessage.getMessage());
        assertThat(clonedMessage.getSource()).isEqualTo(origMessage.getSource());
        assertThat(clonedMessage.getTimestamp()).isEqualTo(origMessage.getTimestamp());
        assertThat(clonedMessage.getStreams()).isEqualTo(origMessage.getStreams());
        assertThat(clonedMessage.hasField("removed_again")).isFalse();
        assertThat(clonedMessage.getFieldAs(Boolean.class, "has_source")).isTrue();
        assertThat(clonedMessage.getFieldAs(String.class, "only_in")).isEqualTo("new message");
        assertThat(clonedMessage.getFieldAs(String.class, "multi")).isEqualTo("new message");
        assertThat(clonedMessage.getFieldAs(String.class, "foo")).isEqualTo("bar");
        assertThat(otherMessage).isNotNull();
        assertThat(otherMessage.getMessage()).isEqualTo("foo");
        assertThat(otherMessage.getSource()).isEqualTo("source");
    }

    @Test
    public void clonedMessageWithInvalidTimestamp() {
        final Message message = new Message("test", "test", Tools.nowUTC());
        message.addField("timestamp", "foobar");
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final EvaluationContext context = contextForRuleEval(rule, message);

        final Message origMessage = context.currentMessage();
        final Message clonedMessage = Iterables.get(context.createdMessages(), 0);

        assertThat(origMessage).isNotEqualTo(clonedMessage);
        assertThat(origMessage.getField("timestamp")).isNotInstanceOf(DateTime.class);

        assertThat(clonedMessage).isNotNull();
        assertThat(clonedMessage.getMessage()).isEqualTo(origMessage.getMessage());
        assertThat(clonedMessage.getSource()).isEqualTo(origMessage.getSource());
        assertThat(clonedMessage.getStreams()).isEqualTo(origMessage.getStreams());
        assertThat(clonedMessage.getTimestamp()).isNotNull();
        assertThat(clonedMessage.getField("gl2_original_timestamp")).isEqualTo(origMessage.getField("timestamp"));
    }

    @Test
    public void grok() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(message).isNotNull();
        assertThat(message.getFieldCount()).isEqualTo(6);
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2015-07-31T10:05:36.773Z"));
        // named captures only
        assertThat(message.hasField("num")).isTrue();
        assertThat(message.hasField("BASE10NUM")).isFalse();

        // Test for issue 5563 and 5794
        // ensure named groups with underscore work
        assertThat(message.hasField("test_field")).isTrue();
    }

    @Test
    public void urls() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
        assertThat(message).isNotNull();
        assertThat(message.getField("protocol")).isEqualTo("https");
        assertThat(message.getField("user_info")).isEqualTo("admin:s3cr31");
        assertThat(message.getField("host")).isEqualTo("some.host.with.lots.of.subdomains.com");
        assertThat(message.getField("port")).isEqualTo(9999);
        assertThat(message.getField("file")).isEqualTo(
                "/path1/path2/three?q1=something&with_spaces=hello%20graylog&equal=can=containanotherone");
        assertThat(message.getField("fragment")).isEqualTo("anchorstuff");
        assertThat(message.getField("query")).isEqualTo(
                "q1=something&with_spaces=hello%20graylog&equal=can=containanotherone");
        assertThat(message.getField("q1")).isEqualTo("something");
        assertThat(message.getField("with_spaces")).isEqualTo("hello graylog");
        assertThat(message.getField("equal")).isEqualTo("can=containanotherone");
        assertThat(message.getField("authority")).isEqualTo("admin:s3cr31@some.host.with.lots.of.subdomains.com:9999");
    }

    @Test
    public void syslog() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message message = evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
        assertThat(message).isNotNull();

        assertThat(message.getField("level0")).isEqualTo("Emergency");
        assertThat(message.getField("level1")).isEqualTo("Alert");
        assertThat(message.getField("level2")).isEqualTo("Critical");
        assertThat(message.getField("level3")).isEqualTo("Error");
        assertThat(message.getField("level4")).isEqualTo("Warning");
        assertThat(message.getField("level5")).isEqualTo("Notice");
        assertThat(message.getField("level6")).isEqualTo("Informational");
        assertThat(message.getField("level7")).isEqualTo("Debug");

        assertThat(message.getField("facility0")).isEqualTo("kern");
        assertThat(message.getField("facility1")).isEqualTo("user");
        assertThat(message.getField("facility2")).isEqualTo("mail");
        assertThat(message.getField("facility3")).isEqualTo("daemon");
        assertThat(message.getField("facility4")).isEqualTo("auth");
        assertThat(message.getField("facility5")).isEqualTo("syslog");
        assertThat(message.getField("facility6")).isEqualTo("lpr");
        assertThat(message.getField("facility7")).isEqualTo("news");
        assertThat(message.getField("facility8")).isEqualTo("uucp");
        assertThat(message.getField("facility9")).isEqualTo("clock");
        assertThat(message.getField("facility10")).isEqualTo("authpriv");
        assertThat(message.getField("facility11")).isEqualTo("ftp");
        assertThat(message.getField("facility12")).isEqualTo("ntp");
        assertThat(message.getField("facility13")).isEqualTo("log audit");
        assertThat(message.getField("facility14")).isEqualTo("log alert");
        assertThat(message.getField("facility15")).isEqualTo("cron");
        assertThat(message.getField("facility16")).isEqualTo("local0");
        assertThat(message.getField("facility17")).isEqualTo("local1");
        assertThat(message.getField("facility18")).isEqualTo("local2");
        assertThat(message.getField("facility19")).isEqualTo("local3");
        assertThat(message.getField("facility20")).isEqualTo("local4");
        assertThat(message.getField("facility21")).isEqualTo("local5");
        assertThat(message.getField("facility22")).isEqualTo("local6");
        assertThat(message.getField("facility23")).isEqualTo("local7");

        assertThat(message.getField("prio1_facility")).isEqualTo(0);
        assertThat(message.getField("prio1_level")).isEqualTo(0);
        assertThat(message.getField("prio2_facility")).isEqualTo(20);
        assertThat(message.getField("prio2_level")).isEqualTo(5);
        assertThat(message.getField("prio3_facility")).isEqualTo("kern");
        assertThat(message.getField("prio3_level")).isEqualTo("Emergency");
        assertThat(message.getField("prio4_facility")).isEqualTo("local4");
        assertThat(message.getField("prio4_level")).isEqualTo("Notice");
    }

    @Test
    public void ipMatchingIssue28() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final Message in = new Message("some message", "somehost.graylog.org", Tools.nowUTC());
        evaluateRule(rule, in);

        assertThat(actionsTriggered.get()).isFalse();
    }

    @Test
    public void fieldRenaming() {
        final Rule rule = parser.parseRule(ruleForTest(), false);

        final Message in = new Message("some message", "somehost.graylog.org", Tools.nowUTC());
        in.addField("field_a", "fieldAContent");
        in.addField("field_b", "not deleted");

        final Message message = evaluateRule(rule, in);

        assertThat(message.hasField("field_1")).isFalse();
        assertThat(message.hasField("field_2")).isTrue();
        assertThat(message.hasField("field_b")).isTrue();
    }

    @Test
    public void comparisons() {
        final Rule rule = parser.parseRule(ruleForTest(), false);
        final EvaluationContext context = contextForRuleEval(rule, new Message("", "", Tools.nowUTC()));
        assertThat(context.hasEvaluationErrors()).isFalse();
        assertThat(evaluateRule(rule)).isNotNull();
        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void conversions() {
        final Rule rule = parser.parseRule(ruleForTest(), false);

        final EvaluationContext context = contextForRuleEval(rule, new Message("test", "test", Tools.nowUTC()));

        assertThat(context.evaluationErrors()).isEmpty();
        final Message message = context.currentMessage();

        assertNotNull(message);
        assertThat(message.getField("string_1")).isEqualTo("1");
        assertThat(message.getField("string_2")).isEqualTo("2");
        // special case, Message doesn't allow adding fields with empty string values
        assertThat(message.hasField("string_3")).isFalse();
        assertThat(message.getField("string_4")).isEqualTo("default");
        assertThat(message.getField("string_5")).isEqualTo("false");
        assertThat(message.getField("string_6")).isEqualTo("42");
        assertThat(message.getField("string_7")).isEqualTo("23.42");

        assertThat(message.getField("long_1")).isEqualTo(1L);
        assertThat(message.getField("long_2")).isEqualTo(2L);
        assertThat(message.getField("long_3")).isEqualTo(0L);
        assertThat(message.getField("long_4")).isEqualTo(1L);
        assertThat(message.getField("long_5")).isEqualTo(23L);
        assertThat(message.getField("long_6")).isEqualTo(23L);
        assertThat(message.getField("long_7")).isEqualTo(1L);
        assertThat(message.getField("long_min1")).isEqualTo(Long.MIN_VALUE);
        assertThat(message.getField("long_min2")).isEqualTo(1L);
        assertThat(message.getField("long_max1")).isEqualTo(Long.MAX_VALUE);
        assertThat(message.getField("long_max2")).isEqualTo(1L);

        assertThat(message.getField("double_1")).isEqualTo(1d);
        assertThat(message.getField("double_2")).isEqualTo(2d);
        assertThat(message.getField("double_3")).isEqualTo(0d);
        assertThat(message.getField("double_4")).isEqualTo(1d);
        assertThat(message.getField("double_5")).isEqualTo(23d);
        assertThat(message.getField("double_6")).isEqualTo(23d);
        assertThat(message.getField("double_7")).isEqualTo(23.42d);
        assertThat(message.getField("double_min1")).isEqualTo(Double.MIN_VALUE);
        assertThat(message.getField("double_min2")).isEqualTo(0d);
        assertThat(message.getField("double_max1")).isEqualTo(Double.MAX_VALUE);
        assertThat(message.getField("double_inf1")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(message.getField("double_inf2")).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(message.getField("double_inf3")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(message.getField("double_inf4")).isEqualTo(Double.NEGATIVE_INFINITY);

        assertThat(message.getField("bool_1")).isEqualTo(true);
        assertThat(message.getField("bool_2")).isEqualTo(false);
        assertThat(message.getField("bool_3")).isEqualTo(false);
        assertThat(message.getField("bool_4")).isEqualTo(true);

        // the is wrapped in our own class for safety in rules
        assertThat(message.getField("ip_1")).isEqualTo(new IpAddress(InetAddresses.forString("127.0.0.1")));
        assertThat(message.getField("ip_2")).isEqualTo(new IpAddress(InetAddresses.forString("127.0.0.1")));
        assertThat(message.getField("ip_3")).isEqualTo(new IpAddress(InetAddresses.forString("0.0.0.0")));
        assertThat(message.getField("ip_4")).isEqualTo(new IpAddress(InetAddresses.forString("::1")));

        assertThat(message.getField("map_1")).isEqualTo(Collections.singletonMap("foo", "bar"));
        assertThat(message.getField("map_2")).isEqualTo(Collections.emptyMap());
        assertThat(message.getField("map_3")).isEqualTo(Collections.emptyMap());
        assertThat(message.getField("map_4")).isEqualTo(Collections.emptyMap());
        assertThat(message.getField("map_5")).isEqualTo(Collections.emptyMap());
        assertThat(message.getField("map_6")).isEqualTo(Collections.emptyMap());
    }

    @Test
    public void fieldPrefixSuffix() {
        final Rule rule = parser.parseRule(ruleForTest(), false);

        final Message message = evaluateRule(rule);

        assertThat(message).isNotNull();

        assertThat(message.getField("field")).isEqualTo("1");
        assertThat(message.getField("prae_field_sueff")).isEqualTo("2");
        assertThat(message.getField("field_sueff")).isEqualTo("3");
        assertThat(message.getField("prae_field")).isEqualTo("4");
        assertThat(message.getField("pre_field1_suff")).isEqualTo("5");
        assertThat(message.getField("pre_field2_suff")).isEqualTo("6");
        assertThat(message.getField("pre_field1")).isEqualTo("7");
        assertThat(message.getField("pre_field2")).isEqualTo("8");
        assertThat(message.getField("field1_suff")).isEqualTo("9");
        assertThat(message.getField("field2_suff")).isEqualTo("10");
    }

    @Test
    public void keyValue() {
        final Rule rule = parser.parseRule(ruleForTest(), true);

        final EvaluationContext context = contextForRuleEval(rule, new Message("", "", Tools.nowUTC()));

        assertThat(context).isNotNull();
        assertThat(context.evaluationErrors()).isEmpty();
        final Message message = context.currentMessage();
        assertThat(message).isNotNull();


        assertThat(message.getField("a")).isEqualTo("1,4");
        assertThat(message.getField("b")).isEqualTo("2");
        assertThat(message.getField("c")).isEqualTo("3");
        assertThat(message.getField("d")).isEqualTo("44");
        assertThat(message.getField("e")).isEqualTo("4");
        assertThat(message.getField("f")).isEqualTo("1");
        assertThat(message.getField("g")).isEqualTo("3");
        assertThat(message.getField("h")).isEqualTo("3=:3");
        assertThat(message.hasField("i")).isFalse();

        assertThat(message.getField("dup_first")).isEqualTo("1");
        assertThat(message.getField("dup_last")).isEqualTo("2");
    }

    @Test
    public void keyValueFailure() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final EvaluationContext context = contextForRuleEval(rule, new Message("", "", Tools.nowUTC()));

        assertThat(context.hasEvaluationErrors()).isTrue();
    }

    @Test
    public void timezones() {
        final InstantMillisProvider clock = new InstantMillisProvider(GRAYLOG_EPOCH);
        DateTimeUtils.setCurrentMillisProvider(clock);
        try {
            final Rule rule = parser.parseRule(ruleForTest(), true);
            evaluateRule(rule);

            assertThat(actionsTriggered.get()).isTrue();
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    public void dateArithmetic() {
        final InstantMillisProvider clock = new InstantMillisProvider(GRAYLOG_EPOCH);
        DateTimeUtils.setCurrentMillisProvider(clock);
        try {
            final Rule rule = parser.parseRule(ruleForTest(), true);
            final Message message = evaluateRule(rule);

            assertThat(actionsTriggered.get()).isTrue();
            assertThat(message).isNotNull();
            assertThat(message.getField("interval"))
                    .isInstanceOf(Duration.class)
                    .matches(o -> ((Duration)o).isEqual(Duration.standardDays(1)), "Exactly one day difference");
            assertThat(message.getField("years")).isEqualTo(Period.years(2));
            assertThat(message.getField("months")).isEqualTo(Period.months(2));
            assertThat(message.getField("weeks")).isEqualTo(Period.weeks(2));
            assertThat(message.getField("days")).isEqualTo(Period.days(2));
            assertThat(message.getField("hours")).isEqualTo(Period.hours(2));
            assertThat(message.getField("minutes")).isEqualTo(Period.minutes(2));
            assertThat(message.getField("seconds")).isEqualTo(Period.seconds(2));
            assertThat(message.getField("millis")).isEqualTo(Period.millis(2));
            assertThat(message.getField("period")).isEqualTo(Period.parse("P1YT1M"));


            assertThat(message.getFieldAs(DateTime.class, "long_time_ago")).matches(date -> date.plus(Period.years(10000)).equals(GRAYLOG_EPOCH));

            assertThat(message.getTimestamp()).isEqualTo(GRAYLOG_EPOCH.plusHours(1));
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    public void routeToStream() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        assertThat(message).isNotNull();
        assertThat(message.getStreams()).isNotEmpty();
        assertThat(message.getStreams().size()).isEqualTo(2);

        final Message message2 = evaluateRule(rule);
        assertThat(message2).isNotNull();
        assertThat(message2.getStreams().size()).isEqualTo(2);
    }

    @Test
    public void routeToStreamRemoveDefault() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        assertThat(message).isNotNull();
        assertThat(message.getStreams()).isNotEmpty();
        assertThat(message.getStreams().size()).isEqualTo(1);

        final Message message2 = evaluateRule(rule);
        assertThat(message2).isNotNull();
        assertThat(message2.getStreams().size()).isEqualTo(1);
    }

    @Test
    public void removeFromStream() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule, msg -> msg.addStream(otherStream));

        assertThat(message).isNotNull();
        assertThat(message.getStreams()).containsOnly(defaultStream);
    }

    @Test
    public void removeFromStreamRetainDefault() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule, msg -> msg.addStream(otherStream));

        assertThat(message).isNotNull();
        assertThat(message.getStreams()).containsOnly(defaultStream);
    }

    @Test
    public void int2ipv4() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        evaluateRule(rule);

        assertThat(actionsTriggered.get()).isTrue();
    }

    @Test
    public void accountingSize() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        // this can change if either the test message content changes or traffic accounting calculation is changed!
        assertThat(message.getField("accounting_size")).isEqualTo(54L);
    }

    @Test
    public void metricCounter() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        evaluateRule(rule);

        assertThat(metricRegistry.getCounters().get("org.graylog.rulemetrics.foo").getCount()).isEqualTo(42);
    }

    @Test
    public void lookupSetValue() {
        doReturn(LookupResult.single(123)).when(lookupTable).setValue(any(), any());

        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        verify(lookupTable).setValue("key", 123L);
        verifyNoMoreInteractions(lookupTable);

        assertThat(message.getField("new_value")).isEqualTo(123);
    }

    @Test
    public void lookupClearKey() {
        // Stub method call to avoid having verifyNoMoreInteractions() fail
        doNothing().when(lookupTable).clearKey(any());

        final Rule rule = parser.parseRule(ruleForTest(), true);
        evaluateRule(rule);

        verify(lookupTable, times(1)).clearKey("key");
        verifyNoMoreInteractions(lookupTable);
    }

    @Test
    public void lookupSetStringList() {
        final ImmutableList<String> testList = ImmutableList.of("foo", "bar");

        doReturn(LookupResult.withoutTTL().stringListValue(testList).build()).when(lookupTable).setStringList(any(), any());

        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        verify(lookupTable).setStringList("key", testList);
        verifyNoMoreInteractions(lookupTable);

        assertThat(message.getField("new_value")).isEqualTo(testList);
    }

    @Test
    public void lookupAddStringList() {
        final ImmutableList<String> testList = ImmutableList.of("foo", "bar");
        doReturn(LookupResult.withoutTTL().stringListValue(testList).build()).when(lookupTable).addStringList(any(), any(), anyBoolean());

        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        verify(lookupTable).addStringList("key", testList, false);
        verifyNoMoreInteractions(lookupTable);

        assertThat(message.getField("new_value")).isEqualTo(testList);
    }

    @Test
    public void lookupRemoveStringList() {
        final ImmutableList<String> testList = ImmutableList.of("foo", "bar");
        final ImmutableList<String> result = ImmutableList.of("bonk");
        doReturn(LookupResult.withoutTTL().stringListValue(result).build()).when(lookupTable).removeStringList(any(), any());

        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        verify(lookupTable).removeStringList("key", testList);
        verifyNoMoreInteractions(lookupTable);

        assertThat(message.getField("new_value")).isEqualTo(result);
    }

    @Test
    public void firstNonNull() {
        final Rule rule = parser.parseRule(ruleForTest(), true);
        final Message message = evaluateRule(rule);

        assertThat(message.getField("not_found")).isNull();
        assertThat(message.getField("first_found")).isEqualTo("first");
        assertThat(message.getField("middle_found")).isEqualTo("middle");
        assertThat(message.getField("last_found")).isEqualTo("last");

        assertThat(message.getField("list_found")).isInstanceOf(List.class);
        assertThat(message.getField("int_found")).isInstanceOf(Long.class);
    }

    @Test
    public void notExpressionTypeCheck() {
        try {
            Rule rule = parser.parseRule(ruleForTest(), true);
            Message in = new Message("test", "source", Tools.nowUTC());
            in.addField("facility", "mail");
            evaluateRule(rule, in);
            fail("missing type check for non-boolean type in unary NOT");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ParseException.class)
                    .hasMessageContaining("Expected type Boolean but found String");
        }
    }
}
