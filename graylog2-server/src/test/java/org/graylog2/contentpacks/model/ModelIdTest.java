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

public class ModelIdTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserialize() {
        final ModelId modelId = ModelId.of("foobar");
        final JsonNode jsonNode = objectMapper.convertValue(modelId, JsonNode.class);

        assertThat(jsonNode.isTextual()).isTrue();
        assertThat(jsonNode.asText()).isEqualTo("foobar");
    }

    @Test
    public void serialize() throws IOException {
        final ModelId modelId = objectMapper.readValue("\"foobar\"", ModelId.class);
        assertThat(modelId).isEqualTo(ModelId.of("foobar"));
    }

    @Test
    public void ensureIdIsNotBlank() {
        assertThatThrownBy(() -> ModelId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID must not be blank");
        assertThatThrownBy(() -> ModelId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID must not be blank");
        assertThatThrownBy(() -> ModelId.of("    \n\r\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID must not be blank");
    }
}