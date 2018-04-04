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
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.grok.GrokPattern;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final GrokPatternConverter converter = new GrokPatternConverter(objectMapper);

    @Test
    public void convert() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final Entity entity = converter.convert(grokPattern);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("grok_pattern"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
        assertThat(data.path("name").asText()).isEqualTo("name");
        assertThat(data.path("pattern").asText()).isEqualTo("pattern");
    }
}