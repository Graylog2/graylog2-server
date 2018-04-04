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
import org.graylog2.grok.GrokPattern;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternExcerptConverterTest {
    private final GrokPatternExcerptConverter converter = new GrokPatternExcerptConverter();

    @Test
    public void convert() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final EntityExcerpt excerpt = converter.convert(grokPattern);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("grok_pattern"));
        assertThat(excerpt.title()).isEqualTo(grokPattern.name());
    }
}