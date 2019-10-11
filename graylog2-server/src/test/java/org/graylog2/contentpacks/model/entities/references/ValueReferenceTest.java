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
package org.graylog2.contentpacks.model.entities.references;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValueReferenceTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void asBoolean() {
        assertThat(ValueReference.of(true).asBoolean(Collections.emptyMap())).isEqualTo(Boolean.TRUE);
        assertThatThrownBy(() -> ValueReference.of("Test").asBoolean(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type BOOLEAN but got STRING");
    }

    @Test
    public void asEnum() {
        assertThat(ValueReference.of(TestEnum.A).asEnum(Collections.emptyMap(), TestEnum.class)).isEqualTo(TestEnum.A);
        assertThatThrownBy(() -> ValueReference.of("Test").asEnum(Collections.emptyMap(), TestEnum.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No enum constant org.graylog2.contentpacks.model.entities.references.ValueReferenceTest.TestEnum.Test");
        assertThatThrownBy(() -> ValueReference.of(0).asEnum(Collections.emptyMap(), TestEnum.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type STRING or PARAMETER but got INTEGER");
    }

    @Test
    public void asFloat() {
        assertThat(ValueReference.of(1.0f).asFloat(Collections.emptyMap())).isEqualTo(1.0f);
        assertThatThrownBy(() -> ValueReference.of("Test").asFloat(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type FLOAT but got STRING");
    }

    @Test
    public void asDouble() {
        assertThat(ValueReference.of(1.0d).asDouble(Collections.emptyMap())).isEqualTo(1.0d);
        assertThatThrownBy(() -> ValueReference.of("Test").asDouble(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type DOUBLE but got STRING");
    }

    @Test
    public void asInteger() {
        assertThat(ValueReference.of(42).asInteger(Collections.emptyMap())).isEqualTo(42);
        assertThatThrownBy(() -> ValueReference.of("Test").asInteger(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type INTEGER but got STRING");
    }

    @Test
    public void asLong() {
        assertThat(ValueReference.of(42L).asLong(Collections.emptyMap())).isEqualTo(42L);
        assertThatThrownBy(() -> ValueReference.of("Test").asLong(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type LONG but got STRING");
    }

    @Test
    public void asString() {
        assertThat(ValueReference.of("Test").asString(Collections.emptyMap())).isEqualTo("Test");
        assertThatThrownBy(() -> ValueReference.of(false).asString(Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected value reference of type STRING but got BOOLEAN");
    }

    @Test
    public void resolveParameter() {
        final ValueReference parameterized = ValueReference.createParameter("parameterized");
        final ValueReference filledStringParameter = ValueReference.of("custom-value");
        final ValueReference integerParameter = ValueReference.of(42);
        assertThat(parameterized.asString(Collections.singletonMap("parameterized", filledStringParameter)))
                .isEqualTo("custom-value");
        assertThat(parameterized.asInteger(Collections.singletonMap("parameterized", integerParameter)))
                .isEqualTo(42);
        assertThatThrownBy(() -> parameterized.asString(Collections.singletonMap("parameterized", integerParameter)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected parameter reference for Java type class java.lang.String but got INTEGER");
    }


    @Test
    public void serializeBoolean() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.of(true))).isEqualTo("{\"@type\":\"boolean\",\"@value\":true}");
        assertThat(objectMapper.writeValueAsString(ValueReference.of(false))).isEqualTo("{\"@type\":\"boolean\",\"@value\":false}");
    }

    @Test
    public void deserializeBoolean() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"boolean\",\"@value\":true}", ValueReference.class)).isEqualTo(ValueReference.of(true));
        assertThat(objectMapper.readValue("{\"@type\":\"boolean\",\"@value\":false}", ValueReference.class)).isEqualTo(ValueReference.of(false));
    }

    @Test
    public void serializeEnum() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.of(TestEnum.A))).isEqualTo("{\"@type\":\"string\",\"@value\":\"A\"}");
        assertThat(objectMapper.writeValueAsString(ValueReference.of(TestEnum.B))).isEqualTo("{\"@type\":\"string\",\"@value\":\"B\"}");
    }

    @Test
    public void deserializeEnum() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"string\",\"@value\":\"A\"}", ValueReference.class)).isEqualTo(ValueReference.of(TestEnum.A));
        assertThat(objectMapper.readValue("{\"@type\":\"string\",\"@value\":\"B\"}", ValueReference.class)).isEqualTo(ValueReference.of(TestEnum.B));
    }

    @Test
    public void serializeFloat() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.of(1.0f))).isEqualTo("{\"@type\":\"float\",\"@value\":1.0}");
        assertThat(objectMapper.writeValueAsString(ValueReference.of(42.4f))).isEqualTo("{\"@type\":\"float\",\"@value\":42.4}");
    }

    @Test
    @Ignore("FIXME: Jackson automatically deserializes floating point numbers as double")
    public void deserializeFloat() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"float\",\"@value\":1.0}", ValueReference.class)).isEqualTo(ValueReference.of(1.0f));
        assertThat(objectMapper.readValue("{\"@type\":\"float\",\"@value\":42.4}", ValueReference.class)).isEqualTo(ValueReference.of(42.4f));
    }

    @Test
    public void serializeInteger() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.of(1))).isEqualTo("{\"@type\":\"integer\",\"@value\":1}");
        assertThat(objectMapper.writeValueAsString(ValueReference.of(42))).isEqualTo("{\"@type\":\"integer\",\"@value\":42}");
    }

    @Test
    public void deserializeInteger() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"integer\",\"@value\":1}", ValueReference.class)).isEqualTo(ValueReference.of(1));
        assertThat(objectMapper.readValue("{\"@type\":\"integer\",\"@value\":42}", ValueReference.class)).isEqualTo(ValueReference.of(42));
    }

    @Test
    public void serializeString() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.of(""))).isEqualTo("{\"@type\":\"string\",\"@value\":\"\"}");
        assertThat(objectMapper.writeValueAsString(ValueReference.of("Test"))).isEqualTo("{\"@type\":\"string\",\"@value\":\"Test\"}");
    }

    @Test
    public void deserializeString() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"string\",\"@value\":\"\"}", ValueReference.class)).isEqualTo(ValueReference.of(""));
        assertThat(objectMapper.readValue("{\"@type\":\"string\",\"@value\":\"Test\"}", ValueReference.class)).isEqualTo(ValueReference.of("Test"));
    }

    @Test
    public void createParameterFailsWithBlankParameter() {
        assertThatThrownBy(() -> ValueReference.createParameter(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parameter must not be blank");
        assertThatThrownBy(() -> ValueReference.createParameter(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parameter must not be blank");
    }

    @Test
    public void serializeParameter() throws IOException {
        assertThat(objectMapper.writeValueAsString(ValueReference.createParameter("Test"))).isEqualTo("{\"@type\":\"parameter\",\"@value\":\"Test\"}");
    }

    @Test
    public void deserializeParameter() throws IOException {
        assertThat(objectMapper.readValue("{\"@type\":\"parameter\",\"@value\":\"Test\"}", ValueReference.class)).isEqualTo(ValueReference.createParameter("Test"));
        assertThatThrownBy(() -> objectMapper.readValue("{\"@type\":\"parameter\",\"@value\":\"\"}", ValueReference.class))
                .isInstanceOf(JsonMappingException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> objectMapper.readValue("{\"@type\":\"parameter\",\"@value\":\" \"}", ValueReference.class))
                .isInstanceOf(JsonMappingException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    enum TestEnum {
        A, B, C
    }
}