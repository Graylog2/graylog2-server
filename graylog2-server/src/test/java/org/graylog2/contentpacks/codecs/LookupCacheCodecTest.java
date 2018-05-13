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
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.LookupCacheEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.lookup.FallbackCacheConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupCacheCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private DBCacheService cacheService;
    private LookupCacheCodec codec;

    @Before
    public void setUp() {
        codec = new LookupCacheCodec(objectMapper, cacheService);
    }

    @Test
    public void encode() {
        final CacheDto cacheDto = CacheDto.builder()
                .id("1234567890")
                .name("cache-name")
                .title("Cache Title")
                .description("Cache Description")
                .config(new FallbackCacheConfig())
                .build();
        final EntityWithConstraints entityWithConstraints = codec.encode(cacheDto);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_cache"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entityV1.data(), LookupCacheEntity.class);
        assertThat(lookupCacheEntity.name()).isEqualTo(ValueReference.of("cache-name"));
        assertThat(lookupCacheEntity.title()).isEqualTo(ValueReference.of("Cache Title"));
        assertThat(lookupCacheEntity.description()).isEqualTo(ValueReference.of("Cache Description"));
        assertThat(lookupCacheEntity.configuration()).containsEntry("type", ValueReference.of("FallbackCacheConfig"));
    }

    @Test
    public void createExcerpt() {
        final CacheDto cacheDto = CacheDto.builder()
                .id("1234567890")
                .name("cache-name")
                .title("Cache Title")
                .description("Cache Description")
                .config(new FallbackCacheConfig())
                .build();
        final EntityExcerpt excerpt = codec.createExcerpt(cacheDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("cache-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_cache"));
        assertThat(excerpt.title()).isEqualTo("Cache Title");
    }
}