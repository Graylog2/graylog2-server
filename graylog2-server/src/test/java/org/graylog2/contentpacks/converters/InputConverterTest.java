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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.inputs.InputImpl;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final InputConverter converter = new InputConverter(objectMapper);

    @Test
    public void convert() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                MessageInput.FIELD_TITLE, "Input Title"
        );
        final InputImpl input = new InputImpl(fields);
        final ImmutableList<Extractor> extractors = ImmutableList.of();
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input, extractors);
        final Entity entity = converter.convert(inputWithExtractors);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(input.getId()));
        assertThat(entity.type()).isEqualTo(ModelType.of("input"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
        // assertThat(data).isEqualTo();
    }
}