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
package org.graylog2.contentpacks.catalogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.converters.GrokPatternConverter;
import org.graylog2.contentpacks.converters.GrokPatternExcerptConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternCatalogTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private InMemoryGrokPatternService grokPatternService;
    private GrokPatternCatalog catalog;

    @Before
    public void setUp() throws Exception {
        grokPatternService = new InMemoryGrokPatternService();
        final GrokPatternExcerptConverter excerptConverter = new GrokPatternExcerptConverter();
        final GrokPatternConverter converter = new GrokPatternConverter(objectMapper);
        catalog = new GrokPatternCatalog(grokPatternService, excerptConverter, converter);
    }

    @Test
    public void supports() {
        assertThat(catalog.supports(ModelType.of("foobar"))).isFalse();
        assertThat(catalog.supports(ModelType.of("grok_pattern"))).isTrue();
    }

    @Test
    public void listEntityExcerpts() throws ValidationException {
        grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        grokPatternService.save(GrokPattern.create("Test2", "[a-z]+"));

        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.GROK_PATTERN)
                .title("Test1")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("2"))
                .type(ModelTypes.GROK_PATTERN)
                .title("Test2")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts)
                .hasSize(2)
                .contains(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    public void collectEntities() throws ValidationException {
        grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        grokPatternService.save(GrokPattern.create("Test2", "[a-z]+"));

        final ObjectNode entityData = objectMapper.createObjectNode()
                .put("name", "Test1")
                .put("pattern", "[a-z]+");
        final Entity expectedEntity = EntityV1.builder()
                .type(ModelTypes.GROK_PATTERN)
                .id(ModelId.of("1"))
                .data(entityData)
                .build();

        final Set<Entity> entityAbstracts = catalog.collectEntities(ImmutableSet.of(ModelId.of("1")));
        assertThat(entityAbstracts)
                .hasSize(1)
                .contains(expectedEntity);
    }
}
