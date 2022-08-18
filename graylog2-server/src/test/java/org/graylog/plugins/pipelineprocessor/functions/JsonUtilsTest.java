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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonFlatten;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonUtilsTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void extractArrays() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"arr1\":[\"a1\",[\"a21\",\"a22\"],[],\"a2\"]}";

        // delete all top-level arrays
        String expected = "{\"k0\":\"v0\"}";
        JsonNode result = JsonUtils.extractJson(jsonString, mapper, JsonFlatten.FLAGS_IGNORE);
        assertThat(mapper.writeValueAsString(result)).isEqualTo(expected);

        // serialize arrays into valid JSON with escaping; retain empty arrays
        expected = "{\"k0\":\"v0\",\"arr1\":\"[\\\"a1\\\",[\\\"a21\\\",\\\"a22\\\"],[],\\\"a2\\\"]\"}";
        result = JsonUtils.extractJson(jsonString, mapper, JsonFlatten.FLAGS_JSON);
        assertThat(mapper.writeValueAsString(result)).isEqualTo(expected);
    }

    @Test
    public void extractObjects() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"obj1\":{\"k1\":\"v1\",\"obj11\":{\"k11\":\"v11\",\"obj111\":{\"k111\":\"v111\"}}}}";

        // flatten objects by concatenating keys
        String expected = "{\"k0\":\"v0\",\"obj1_k1\":\"v1\",\"obj1_obj11_k11\":\"v11\",\"obj1_obj11_obj111_k111\":\"v111\"}";
        JsonNode result = JsonUtils.extractJson(jsonString, mapper, JsonFlatten.FLAGS_IGNORE);
        assertThat(mapper.writeValueAsString(result)).isEqualTo(expected);
    }

    @Test
    public void extractArrayOfObjects() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"arr1\":[{\"k11\":\"v11\",\"k12\":\"v12\"},{\"k21\":\"v21\"}]}";

        // flatten objects by concatenating keys
        String expected = "{\"k0\":\"v0\",\"arr1_0_k11\":\"v11\",\"arr1_0_k12\":\"v12\",\"arr1_1_k21\":\"v21\"}";
        JsonNode result = JsonUtils.extractJson(jsonString, mapper, JsonFlatten.FLAGS_FLATTEN);
        assertThat(mapper.writeValueAsString(result)).isEqualTo(expected);
    }

    @Test
    public void deleteLevel1Object() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"obj1\":{\"k1\":\"v1\"}}";
        ObjectMapper mapper = new ObjectMapper();

        // NOOP if maxdepth == 0
        JsonNode node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 0);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // NOOP if maxdepth >= actual maximum depth
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 2);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // Remove all container nodes from top level
        String expected = "{\"k0\":\"v0\"}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 1);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);
    }

    @Test
    public void deleteLevel1Array() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"arr1\":[\"a1\",\"a2\"]}";
        ObjectMapper mapper = new ObjectMapper();

        // NOOP if maxdepth == 0
        JsonNode node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 0);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // NOOP if maxdepth >= actual maximum depth
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 2);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // Remove all container nodes from top level
        String expected = "{\"k0\":\"v0\"}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 1);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);
    }

    @Test
    public void deleteNestedArray() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"arr1\":[[],\"a1\",[\"a21\",\"a22\"],[],\"a2\",[]]}";
        ObjectMapper mapper = new ObjectMapper();

        // NOOP if maxdepth == 0
        JsonNode node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 0);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // NOOP if maxdepth >= actual maximum depth
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 3);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // Remove all container nodes from top level
        String expected = "{\"k0\":\"v0\"}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 1);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);

        // Remove singly nested container nodes
        expected = "{\"k0\":\"v0\",\"arr1\":[\"a1\",\"a2\"]}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 2);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);
    }

    @Test
    public void deleteNestedObject() throws IOException {
        String jsonString = "{\"k0\":\"v0\",\"obj1\":{\"k1\":\"v1\",\"obj11\":{\"k11\":\"v11\",\"obj111\":{\"k111\":\"v111\"}}}}";
        ObjectMapper mapper = new ObjectMapper();

        // NOOP if maxdepth == 0
        JsonNode node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 0);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // NOOP if maxdepth >= actual maximum depth
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 4);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(jsonString);

        // Remove all container nodes from top level
        String expected = "{\"k0\":\"v0\"}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 1);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);

        // Remove singly nested container nodes
        expected = "{\"k0\":\"v0\",\"obj1\":{\"k1\":\"v1\"}}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 2);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);

        // Remove doubly nested container nodes
        expected = "{\"k0\":\"v0\",\"obj1\":{\"k1\":\"v1\",\"obj11\":{\"k11\":\"v11\"}}}";
        node = mapper.readTree(jsonString);
        JsonUtils.deleteBelow(node, 3);
        assertThat(mapper.writeValueAsString(node)).isEqualTo(expected);
    }
}
