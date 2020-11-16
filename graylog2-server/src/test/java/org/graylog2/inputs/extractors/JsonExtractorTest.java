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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonExtractorTest {
    private JsonExtractor jsonExtractor;

    @Before
    public void setUp() throws Extractor.ReservedFieldException, ConfigurationException {
        jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", Collections.<String, Object>emptyMap(), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
    }

    @Test(expected = ConfigurationException.class)
    public void constructorFailsOnMissingConfiguration() throws Extractor.ReservedFieldException, ConfigurationException {
        new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", null, "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE, "");
    }

    @Test
    public void testRunWithNullInput() throws Exception {
        assertThat(jsonExtractor.run(null)).isEmpty();
    }

    @Test
    public void testRunWithEmptyInput() throws Exception {
        assertThat(jsonExtractor.run("")).isEmpty();
    }

    @Test
    public void testRunWithScalarValues() throws Exception {
        final String value = "{\"text\": \"foobar\", \"number\": 1234.5678, \"bool\": true, \"null\": null}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
                new Extractor.Result("foobar", "text", -1, -1),
                new Extractor.Result(1234.5678, "number", -1, -1),
                new Extractor.Result(true, "bool", -1, -1)
        );

        // Null values should be ignored!
        assertThat(results).doesNotContain(new Extractor.Result(null, "null", -1, -1));
    }

    @Test
    public void testRunWithArray() throws Exception {
        final String value = "{\"array\": [\"foobar\", \"foobaz\", null]}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(new Extractor.Result("foobar, foobaz", "array", -1, -1));
    }

    @Test
    public void testRunWithArrayAndDifferentListSeparator() throws Exception {
        final JsonExtractor jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.<String, Object>of("list_separator", ":"), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
        final String value = "{\"array\": [\"foobar\", \"foobaz\"]}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(new Extractor.Result("foobar:foobaz", "array", -1, -1));
    }

    @Test
    public void testRunWithObject() throws Exception {
        final String value = "{\"object\": {\"text\": \"foobar\", \"number\": 1234.5678, \"bool\": true, \"nested\": {\"text\": \"foobar\"}}}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
                new Extractor.Result("foobar", "object_text", -1, -1),
                new Extractor.Result(1234.5678, "object_number", -1, -1),
                new Extractor.Result(true, "object_bool", -1, -1),
                new Extractor.Result("foobar", "object_nested_text", -1, -1)
        );
    }

    @Test
    public void testRunWithFlattenedObject() throws Exception {
        final JsonExtractor jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.<String, Object>of("flatten", true), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
        final String value = "{"
                + "\"object\": {"
                + "\"text\": \"foobar\", "
                + "\"number\": 1234.5678, "
                + "\"bool\": true, "
                + "\"null\": null, "
                + "\"nested\": {\"text\": \"foobar\"}"
                + "}"
                + "}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
                new Extractor.Result("text=foobar, number=1234.5678, bool=true, nested={text=foobar}", "object", -1, -1)
        );
    }

    @Test
    public void testRunWithObjectAndDifferentKeySeparator() throws Exception {
        final JsonExtractor jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.<String, Object>of("key_separator", ":"), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
        final String value = "{\"object\": {\"text\": \"foobar\", \"number\": 1234.5678, \"bool\": true, \"nested\": {\"text\": \"foobar\"}}}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
                new Extractor.Result("foobar", "object:text", -1, -1),
                new Extractor.Result(1234.5678, "object:number", -1, -1),
                new Extractor.Result(true, "object:bool", -1, -1),
                new Extractor.Result("foobar", "object:nested:text", -1, -1)
        );
    }

    @Test
    public void testRunWithFlattenedObjectAndDifferentKVSeparator() throws Exception {
        final JsonExtractor jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.<String, Object>of("flatten", true, "kv_separator", ":"), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
        final String value = "{\"object\": {\"text\": \"foobar\", \"number\": 1234.5678, \"bool\": true, \"nested\": {\"text\": \"foobar\"}}}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
                new Extractor.Result("text:foobar, number:1234.5678, bool:true, nested:{text=foobar}", "object", -1, -1)
        );
    }

    @Test
    public void issue2375() throws Exception {
        final String value = "{\"Source\" : \"Myserver#DS\",\"Observer\" : {\"Account\" : {\"Domain\" : \"MYDOMAIN\",\"Name\" : \"CN=domain,OU=service,O=org,C=DE\"},\"Entity\" : {\"SysAddr\" : \"123.123.123.123\",\"SysName\" : \"dir\"}},\"Initiator\" : {\"Account\" : {\"Domain\" : \"Domain\"},\"Entity\" : {\"SysAddr\" : \"0.0.0.0:0\"}},\"Target\" : {\"Data\" : {\"Attribute Name\" : \"Purge Vector\",\"Attribute Value\" : \"Seconds: 1466090058, Replica Number: 3, Event: 1\",\"ClassName\" : \"Organizational Unit\",\"Syntax\" : \"19\"},\"Account\" : {\"Domain\" : \"Domain\",\"Name\" : \"OU=myOrg,O=org,C=DE\",\"Id\" : \"34621\"}},\"Action\" : {\"Event\" : {\"Id\" : \"0.0.0.2\",\"Name\" : \"DISABLE_ACCOUNT\",\"CorrelationID\" : \"eDirectory#0#6b2c7a3d-a223-19da-85ad-3d7a1c6b82a3\",\"SubEvent\" : \"DSE_ADD_VALUE\"},\"Time\" : {\"Offset\" : 1466090106},\"Log\" : {\"Severity\" : 7},\"Outcome\" : \"0\",\"ExtendedOutcome\" : \"0\"}}";
        final Extractor.Result[] results = jsonExtractor.run(value);

        assertThat(results).contains(
            new Extractor.Result("Myserver#DS", "Source", -1, -1),
            new Extractor.Result("Purge Vector", "Target_Data_Attribute Name", -1, -1)
        );
    }

    @Test
    public void testRunWithWhitespaceInKey() throws Exception {
        final String value = "{\"text string\": \"foobar\", \"num   b er\": 1234.5678, \"bool\": true, \"null\": null}";

        final JsonExtractor jsonExtractor1 = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", Collections.emptyMap(), "user", Collections.emptyList(), Extractor.ConditionType.NONE,
                "");

        final JsonExtractor jsonExtractor2 = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.of("replace_key_whitespace", true), "user", Collections.emptyList(), Extractor.ConditionType.NONE,
                "");

        final JsonExtractor jsonExtractor3 = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.of("replace_key_whitespace", true, "key_whitespace_replacement", ":"), "user", Collections.emptyList(), Extractor.ConditionType.NONE,
                "");

        assertThat(jsonExtractor1.run(value)).contains(
                new Extractor.Result("foobar", "text string", -1, -1),
                new Extractor.Result(1234.5678, "num   b er", -1, -1),
                new Extractor.Result(true, "bool", -1, -1)
        );
        assertThat(jsonExtractor2.run(value)).contains(
                new Extractor.Result("foobar", "text_string", -1, -1),
                new Extractor.Result(1234.5678, "num___b_er", -1, -1),
                new Extractor.Result(true, "bool", -1, -1)
        );
        assertThat(jsonExtractor3.run(value)).contains(
                new Extractor.Result("foobar", "text:string", -1, -1),
                new Extractor.Result(1234.5678, "num:::b:er", -1, -1),
                new Extractor.Result(true, "bool", -1, -1)
        );
    }

    @Test
    public void testRunWithWhitespaceInNestedKey() throws Exception {
        final String value = "{\"foo\":{\"b a r\":{\"b a z\": 42}}}";
        final JsonExtractor jsonExtractor = new JsonExtractor(
                new MetricRegistry(),
                "json",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "source",
                "target",
                ImmutableMap.of("replace_key_whitespace", true, "key_whitespace_replacement", "-"),
                "user",
                Collections.emptyList(),
                Extractor.ConditionType.NONE,
                "");

        assertThat(jsonExtractor.run(value)).containsOnly(
                new Extractor.Result(42, "foo_b-a-r_b-a-z", -1, -1)
        );
    }

    @Test
    public void testRunWithKeyPrefix() throws Exception {
        final String value = "{\"text string\": \"foobar\", \"num   b er\": 1234.5678, \"bool\": true, \"null\": null}";

        final JsonExtractor jsonExtractor1 = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.of("key_prefix", "test_"), "user", Collections.emptyList(), Extractor.ConditionType.NONE,
                "");

        assertThat(jsonExtractor1.run(value)).contains(
                new Extractor.Result("foobar", "test_text string", -1, -1),
                new Extractor.Result(1234.5678, "test_num   b er", -1, -1),
                new Extractor.Result(true, "test_bool", -1, -1)
        );
    }
}
