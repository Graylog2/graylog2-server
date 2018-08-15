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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ModelTypeTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserialize() {
        final ModelType modelType = ModelType.of("foobar", "1");
        final JsonNode jsonNode = objectMapper.convertValue(modelType, JsonNode.class);

        assertThat(jsonNode.isObject()).isTrue();
        assertThat(jsonNode.path("name").asText()).isEqualTo("foobar");
        assertThat(jsonNode.path("version").asText()).isEqualTo("1");
    }

    @Test
    public void serialize() throws IOException {
        final ModelType modelType = objectMapper.readValue("{\"name\":\"foobar\",\"version\":\"1\"}", ModelType.class);
        assertThat(modelType).isEqualTo(ModelType.of("foobar", "1"));
    }

    @Test
    public void ensureTypeIsNotBlank() {
        assertThatThrownBy(() -> ModelType.of(null, "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type name must not be blank");
        assertThatThrownBy(() -> ModelType.of("", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type name must not be blank");
        assertThatThrownBy(() -> ModelType.of("    \n\r\t", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type name must not be blank");

        assertThatThrownBy(() -> ModelType.of("foo", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type version must not be blank");
        assertThatThrownBy(() -> ModelType.of("foo", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type version must not be blank");
        assertThatThrownBy(() -> ModelType.of("foo", "    \n\r\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type version must not be blank");
    }
}