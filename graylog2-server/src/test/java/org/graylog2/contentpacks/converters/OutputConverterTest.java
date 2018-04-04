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
package org.graylog2.contentpacks.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class OutputConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final OutputConverter converter = new OutputConverter(objectMapper);

    @Test
    public void convert() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of(
                "some-setting", "foobar"
        );
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.output.SomeOutputClass",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final Entity entity = converter.convert(output);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("output"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
        assertThat(data.path("title").asText()).isEqualTo("Output Title");
        assertThat(data.path("type").asText()).isEqualTo("org.graylog2.output.SomeOutputClass");
        assertThat(data.path("configuration").isObject()).isTrue();
        assertThat(data.path("configuration").path("some-setting").asText()).isEqualTo("foobar");
    }
}