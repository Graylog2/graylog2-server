package org.graylog.plugins.pipelineprocessor.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonUtilsTest {
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
