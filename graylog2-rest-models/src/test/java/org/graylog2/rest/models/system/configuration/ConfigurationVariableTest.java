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
package org.graylog2.rest.models.system.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationVariableTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeserializeString() throws IOException {
        final String json = "{\"name\":\"string\",\"value\":\"foobar\"}";
        final ConfigurationVariable configurationVariable = objectMapper.readValue(json, ConfigurationVariable.class);

        assertThat(configurationVariable.name()).isEqualTo("string");
        assertThat(configurationVariable.value()).isEqualTo("foobar");
    }

    @Test
    public void testDeserializeNumber() throws IOException {
        final String json = "{\"name\":\"number\",\"value\":42}";
        final ConfigurationVariable configurationVariable = objectMapper.readValue(json, ConfigurationVariable.class);

        assertThat(configurationVariable.name()).isEqualTo("number");
        assertThat(configurationVariable.value()).isEqualTo(42);
    }

    @Test
    public void testDeserializeBoolean() throws IOException {
        final String json = "{\"name\":\"boolean\",\"value\":true}";
        final ConfigurationVariable configurationVariable = objectMapper.readValue(json, ConfigurationVariable.class);

        assertThat(configurationVariable.name()).isEqualTo("boolean");
        assertThat(configurationVariable.value()).isEqualTo(true);
    }

    @Test
    public void testSerializeString() throws IOException {
        final ConfigurationVariable configurationVariable = ConfigurationVariable.create("string", "foobar");
        final String json = objectMapper.writeValueAsString(configurationVariable);

        assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"string\",\"value\":\"foobar\"}");
    }

    @Test
    public void testSerializeNumber() throws IOException {
        final ConfigurationVariable configurationVariable = ConfigurationVariable.create("number", 42);
        final String json = objectMapper.writeValueAsString(configurationVariable);

        assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"number\",\"value\":42}");

    }

    @Test
    public void testSerializeBoolean() throws IOException {
        final ConfigurationVariable configurationVariable = ConfigurationVariable.create("boolean", true);
        final String json = objectMapper.writeValueAsString(configurationVariable);

        assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"boolean\",\"value\":true}");
    }
}