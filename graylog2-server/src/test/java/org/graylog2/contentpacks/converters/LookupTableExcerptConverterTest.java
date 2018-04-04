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
import org.graylog2.lookup.LookupDefaultValue;
import org.graylog2.lookup.dto.LookupTableDto;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupTableExcerptConverterTest {
    private final LookupTableExcerptConverter converter = new LookupTableExcerptConverter();

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
        final EntityExcerpt excerpt = converter.convert(lookupTableDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("lookup-table-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_table"));
        assertThat(excerpt.title()).isEqualTo("Lookup Table Title");
    }
}