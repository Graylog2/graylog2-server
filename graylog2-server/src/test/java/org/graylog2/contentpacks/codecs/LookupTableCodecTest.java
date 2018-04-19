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
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.lookup.LookupDefaultValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupTableCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private DBLookupTableService lookupTableService;
    private LookupTableCodec codec;

    @Before
    public void setUp() {
        codec = new LookupTableCodec(objectMapper, lookupTableService);
    }

    @Test
    public void encode() {
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
        final Entity entity = codec.encode(lookupTableDto);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_table"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entityV1.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo("lookup-table-name");
        assertThat(lookupTableEntity.title()).isEqualTo("Lookup Table Title");
        assertThat(lookupTableEntity.description()).isEqualTo("Lookup Table Description");
        assertThat(lookupTableEntity.dataAdapterName()).isEqualTo("data-adapter-1234");
        assertThat(lookupTableEntity.cacheName()).isEqualTo("cache-1234");
        assertThat(lookupTableEntity.defaultSingleValue()).isEqualTo("default-single");
        assertThat(lookupTableEntity.defaultSingleValueType()).isEqualTo(LookupDefaultValue.Type.STRING);
        assertThat(lookupTableEntity.defaultMultiValue()).isEqualTo("default-multi");
        assertThat(lookupTableEntity.defaultMultiValueType()).isEqualTo(LookupDefaultValue.Type.STRING);
    }

    @Test
    public void createExcerpt() {
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
        final EntityExcerpt excerpt = codec.createExcerpt(lookupTableDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("lookup-table-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_table"));
        assertThat(excerpt.title()).isEqualTo("Lookup Table Title");
    }
}