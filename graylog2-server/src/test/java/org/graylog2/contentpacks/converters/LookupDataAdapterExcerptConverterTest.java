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
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupDataAdapterExcerptConverterTest {
    private final LookupDataAdapterExcerptConverter converter = new LookupDataAdapterExcerptConverter();

    @Test
    public void convert() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityExcerpt excerpt = converter.convert(dataAdapterDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("data-adapter-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_adapter"));
        assertThat(excerpt.title()).isEqualTo("Data Adapter Title");
    }
}