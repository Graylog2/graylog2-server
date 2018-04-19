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
import org.graylog2.contentpacks.model.entities.GrokPatternEntity;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternCodecTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private InMemoryGrokPatternService grokPatternService;
    private GrokPatternCodec codec;

    @Before
    public void setUp() {
        grokPatternService = new InMemoryGrokPatternService();
        codec = new GrokPatternCodec(objectMapper, grokPatternService);
    }

    @Test
    public void encode() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final Entity entity = codec.encode(grokPattern);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("grok_pattern"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final GrokPatternEntity grokPatternEntity = objectMapper.convertValue(entityV1.data(), GrokPatternEntity.class);
        assertThat(grokPatternEntity.name()).isEqualTo("name");
        assertThat(grokPatternEntity.pattern()).isEqualTo("pattern");
    }

    @Test
    public void createExcerpt() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final EntityExcerpt excerpt = codec.createExcerpt(grokPattern);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("grok_pattern"));
        assertThat(excerpt.title()).isEqualTo(grokPattern.name());
    }
}