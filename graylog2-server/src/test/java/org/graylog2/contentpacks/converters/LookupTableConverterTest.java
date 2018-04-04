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
import org.graylog2.lookup.LookupDefaultValue;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupTableConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final LookupTableConverter converter = new LookupTableConverter(objectMapper);

    @Test
    public void convert() {
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .id("1234567890")
                .name("lookup-table-name")
                .title("Lookup Table Title")
                .description("Lookup Table Description")
                .dataAdapterId("data-adapter-1234")
                .cacheId("cache-1234")
                .defaultSingleValue("default-single")
                .defaultSingleValueType(LookupDefaultValue.Type.STRING)
                .defaultMultiValue("default-multi")
                .defaultMultiValueType(LookupDefaultValue.Type.STRING)
                .build();
        final Entity entity = converter.convert(lookupTableDto);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_table"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
        assertThat(data.path("name").asText()).isEqualTo("lookup-table-name");
        assertThat(data.path("title").asText()).isEqualTo("Lookup Table Title");
        assertThat(data.path("description").asText()).isEqualTo("Lookup Table Description");
        assertThat(data.path("adapter_id").asText()).isEqualTo("data-adapter-1234");
        assertThat(data.path("cache_id").asText()).isEqualTo("cache-1234");
        assertThat(data.path("default_single_value").asText()).isEqualTo("default-single");
        assertThat(data.path("default_single_value_type").asText()).isEqualTo("STRING");
        assertThat(data.path("default_multi_value").asText()).isEqualTo("default-multi");
        assertThat(data.path("default_multi_value_type").asText()).isEqualTo("STRING");
    }
}