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

import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.lookup.FallbackCacheConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupCacheExcerptConverterTest {
    private final LookupCacheExcerptConverter converter = new LookupCacheExcerptConverter();

    @Test
    public void convert() {
        final CacheDto cacheDto = CacheDto.builder()
                .id("1234567890")
                .name("cache-name")
                .title("Cache Title")
                .description("Cache Description")
                .config(new FallbackCacheConfig())
                .build();
        final EntityExcerpt excerpt = converter.convert(cacheDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("cache-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_cache"));
        assertThat(excerpt.title()).isEqualTo("Cache Title");
    }
}