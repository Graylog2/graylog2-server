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
import org.graylog2.contentpacks.codecs.GrokPatternCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternCatalogTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private InMemoryGrokPatternService grokPatternService;
    private GrokPatternCatalog catalog;

    @Before
    public void setUp() throws Exception {
        grokPatternService = new InMemoryGrokPatternService();
        final GrokPatternCodec codec = new GrokPatternCodec(objectMapper, grokPatternService);
        catalog = new GrokPatternCatalog(grokPatternService, codec);
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
    public void collectEntity() throws ValidationException {
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

        final Optional<EntityWithConstraints> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("1"), ModelTypes.GROK_PATTERN));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .contains(expectedEntity);
    }
}
