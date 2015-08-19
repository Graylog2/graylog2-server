/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
                new Extractor.Result(true, "bool", -1, -1),
                new Extractor.Result(null, "null", -1, -1)
        );
    }

    @Test
    public void testRunWithArray() throws Exception {
        final String value = "{\"array\": [\"foobar\", \"foobaz\"]}";
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
                new Extractor.Result("foobar", "object.text", -1, -1),
                new Extractor.Result(1234.5678, "object.number", -1, -1),
                new Extractor.Result(true, "object.bool", -1, -1),
                new Extractor.Result("foobar", "object.nested.text", -1, -1)
        );
    }

    @Test
    public void testRunWithFlattenedObject() throws Exception {
        final JsonExtractor jsonExtractor = new JsonExtractor(new MetricRegistry(), "json", "title", 0L, Extractor.CursorStrategy.COPY,
                "source", "target", ImmutableMap.<String, Object>of("flatten", true), "user", Collections.<Converter>emptyList(), Extractor.ConditionType.NONE,
                "");
        final String value = "{\"object\": {\"text\": \"foobar\", \"number\": 1234.5678, \"bool\": true, \"nested\": {\"text\": \"foobar\"}}}";
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
}