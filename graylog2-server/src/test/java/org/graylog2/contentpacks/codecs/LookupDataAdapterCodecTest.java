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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupDataAdapterCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private DBDataAdapterService dataAdapterService;
    private LookupDataAdapterCodec codec;

    @Before
    public void setUp() {
        codec = new LookupDataAdapterCodec(objectMapper, dataAdapterService);
    }

    @Test
    public void encode() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final Entity entity = codec.encode(dataAdapterDto);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dataAdapterDto.id()));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_adapter"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo("data-adapter-name");
        assertThat(lookupDataAdapterEntity.title()).isEqualTo("Data Adapter Title");
        assertThat(lookupDataAdapterEntity.description()).isEqualTo("Data Adapter Description");
        assertThat(lookupDataAdapterEntity.configuration()).containsEntry("type", null);
    }

    @Test
    public void createExcerpt() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityExcerpt excerpt = codec.createExcerpt(dataAdapterDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("data-adapter-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_adapter"));
        assertThat(excerpt.title()).isEqualTo("Data Adapter Title");
    }
}