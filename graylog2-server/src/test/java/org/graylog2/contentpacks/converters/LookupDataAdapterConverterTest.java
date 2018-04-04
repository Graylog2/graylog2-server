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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupDataAdapterConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final LookupDataAdapterConverter converter = new LookupDataAdapterConverter(objectMapper);

    @Test
    public void convert() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final Entity entity = converter.convert(dataAdapterDto);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dataAdapterDto.id()));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_adapter"));

        final EntityV1 entityV1 = (EntityV1) entity;
        // assertThat(entityV1.data()).isEqualTo();
    }
}