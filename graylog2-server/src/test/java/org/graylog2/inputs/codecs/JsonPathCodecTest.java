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
package org.graylog2.inputs.codecs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.inputs.codecs.JsonPathCodec.CK_FLATTEN;
import static org.graylog2.inputs.codecs.JsonPathCodec.CK_PATH;

// TODO migrate test to use codec instead
public class JsonPathCodecTest {

    private static final double DELTA = 1e-15;
    private final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
    private final MessageFactory messageFactory = new TestMessageFactory();

    private static Configuration configOf(String key, Object value) {
        Map<String, Object> vals = Map.of(key, value);
        return new Configuration(vals);
    }

    private static Configuration configOf(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> vals = Map.of(key1, value1, key2, value2);
        return new Configuration(vals);
    }

    @Test
    public void testReadResultingInSingleInteger() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"download_count\":76185,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        String path = "$.download_count";

        Map<String, Object> result = new JsonPathCodec(configOf(CK_PATH, path), objectMapperProvider.get(), messageFactory).read(json);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("result")).isEqualTo(76185);
    }

    @Test
    public void testReadResultingInSingleIntegerFullJson() throws Exception {
        RawMessage json = new RawMessage("{\"download_count\":76185}".getBytes(StandardCharsets.UTF_8));
        String path = "$.download_count";

        Message result = new JsonPathCodec(configOf(CK_PATH, path, CK_FLATTEN, true), objectMapperProvider.get(), messageFactory).decode(json);
        assertThat(result.getField("download_count")).isEqualTo(76185);
    }

    @Test
    public void testReadResultingInSingleString() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"download_count\":76185,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        String path = "$.state";

        Map<String, Object> result = new JsonPathCodec(configOf(CK_PATH, path), objectMapperProvider.get(), messageFactory).read(json);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("result")).isEqualTo("uploaded");
    }

    @Test
    public void testReadResultingInSingleStringFullJson() throws Exception {
        RawMessage json = new RawMessage("{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"download_count\":76185,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}".getBytes(StandardCharsets.UTF_8));
        String path = "$.state";

        Message result = new JsonPathCodec(configOf(CK_PATH, path, CK_FLATTEN, true), objectMapperProvider.get(), messageFactory).decode(json);
        assertThat(result.getField("state")).isEqualTo("\"uploaded\"");
    }

    @Test
    public void testReadFromMap() throws Exception {
        String json = "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}}}";
        String path = "$.store.book[?(@.category == 'fiction')].author";

        Map<String, Object> result = new JsonPathCodec(configOf(CK_PATH, path), objectMapperProvider.get(), messageFactory).read(json);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("result")).isEqualTo("Evelyn Waugh");
    }

    @Test
    public void testReadFromMapFullJson() throws Exception {
        RawMessage json = new RawMessage("{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}}}".getBytes(StandardCharsets.UTF_8));
        String path = "$.store.book[?(@.category == 'fiction')].author";

        Message result = new JsonPathCodec(configOf(CK_PATH, path, CK_FLATTEN, true), objectMapperProvider.get(), messageFactory).decode(json);
        assertThat(result.getField("store.book1.author")).isEqualTo("\"Evelyn Waugh\"");
    }

    @Test
    public void testReadResultingInDouble() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"some_double\":0.50,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        String path = "$.some_double";

        Map<String, Object> result = new JsonPathCodec(configOf(CK_PATH, path), objectMapperProvider.get(), messageFactory).read(json);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("result")).isEqualTo(0.5);
    }

    @Test
    public void testReadResultingInDoubleFullJson() throws Exception {
        RawMessage json = new RawMessage("{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"some_double\":0.50,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}".getBytes(StandardCharsets.UTF_8));
        String path = "$.store.book[?(@.category == 'fiction')].author";

        Message result = new JsonPathCodec(configOf(CK_PATH, path, CK_FLATTEN, true), objectMapperProvider.get(), messageFactory).decode(json);
        assertThat(result.getField("some_double")).isEqualTo(0.5);
    }

    @Test
    public void testBuildShortMessage() throws Exception {
        Map<String, Object> fields = Maps.newLinkedHashMap();
        fields.put("baz", 9001);
        fields.put("foo", "bar");

        JsonPathCodec selector = new JsonPathCodec(configOf(CK_PATH, "$.download_count", CK_FLATTEN, false), objectMapperProvider.get(), messageFactory);
        assertThat(selector.buildShortMessage(fields)).isEqualTo("JSON API poll result: $['download_count'] -> {baz=9001, foo=bar}");
    }

    @Test
    public void testBuildShortMessageFullJson() throws Exception {
        Map<String, Object> fields = Maps.newLinkedHashMap();
        fields.put("baz", 9001);
        fields.put("foo", "bar");

        JsonPathCodec selector = new JsonPathCodec(configOf(CK_PATH, "$.download_count", CK_FLATTEN, true), objectMapperProvider.get(), messageFactory);
        assertThat(selector.buildShortMessage(fields)).isEqualTo("JSON API poll result:  -> {baz=9001, foo=bar}");
    }

    @Test
    public void testBuildShortMessageThatGetsCut() throws Exception {
        Map<String, Object> fields = Maps.newLinkedHashMap();
        fields.put("baz", 9001);
        fields.put("foo", "bargggdzrtdfgfdgldfsjgkfdlgjdflkjglfdjgljslfperitperoujglkdnfkndsbafdofhasdpfoöadjsFOO");

        JsonPathCodec selector = new JsonPathCodec(configOf(CK_PATH, "$.download_count", CK_FLATTEN, false), objectMapperProvider.get(), messageFactory);
        assertThat(selector.buildShortMessage(fields)).isEqualTo("JSON API poll result: $['download_count'] -> {baz=9001, foo=bargggdzrtdfgfdgldfsjgkfdlgjdflkjgl[...]");
    }

    @Test
    public void testBuildShortMessageThatGetsCutFullJson() throws Exception {
        Map<String, Object> fields = Maps.newLinkedHashMap();
        fields.put("baz", 9001);
        fields.put("foo", "bargggdzrtdfgfdgldfsjgkfdlgjdflkjglfdjgljslfperitperoujglkdnfkndsbafdofhasdpfoöadjsFOO");

        JsonPathCodec selector = new JsonPathCodec(configOf(CK_PATH, "$.download_count", CK_FLATTEN, true), objectMapperProvider.get(), messageFactory);
        assertThat(selector.buildShortMessage(fields)).isEqualTo("JSON API poll result:  -> {baz=9001, foo=bargggdzrtdfgfdgldfsjgkfdlgjdflkjgl[...]");
    }

}
