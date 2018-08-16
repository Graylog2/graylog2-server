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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.GrokPatternEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GrokPatternFacadeTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private InMemoryGrokPatternService grokPatternService;
    private GrokPatternFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        grokPatternService = new InMemoryGrokPatternService(clusterEventBus);
        facade = new GrokPatternFacade(objectMapper, grokPatternService);
    }

    @Test
    public void exportNativeEntity() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(grokPattern);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelTypes.GROK_PATTERN_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final GrokPatternEntity grokPatternEntity = objectMapper.convertValue(entityV1.data(), GrokPatternEntity.class);
        assertThat(grokPatternEntity.name()).isEqualTo(ValueReference.of("name"));
        assertThat(grokPatternEntity.pattern()).isEqualTo(ValueReference.of("pattern"));
    }

    @Test
    public void createExcerpt() {
        final GrokPattern grokPattern = GrokPattern.create("01234567890", "name", "pattern", null);
        final EntityExcerpt excerpt = facade.createExcerpt(grokPattern);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.GROK_PATTERN_V1);
        assertThat(excerpt.title()).isEqualTo(grokPattern.name());
    }

    @Test
    public void listEntityExcerpts() throws ValidationException {
        grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        grokPatternService.save(GrokPattern.create("Test2", "[a-z]+"));

        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .title("Test1")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("2"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .title("Test2")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts)
                .hasSize(2)
                .contains(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    public void exportEntity() throws ValidationException {
        grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        grokPatternService.save(GrokPattern.create("Test2", "[a-z]+"));

        final Map<String, Object> entity = ImmutableMap.of(
                "name", ValueReference.of("Test1"),
                "pattern", ValueReference.of("[a-z]+"));
        final JsonNode entityData = objectMapper.convertValue(entity, JsonNode.class);
        final Entity expectedEntity = EntityV1.builder()
                .type(ModelTypes.GROK_PATTERN_V1)
                .id(ModelId.of("1"))
                .data(entityData)
                .build();

        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("1", ModelTypes.GROK_PATTERN_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .contains(expectedEntity);
    }

    @Test
    public void delete() throws ValidationException {
        final GrokPattern grokPattern = grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        grokPatternService.save(GrokPattern.create("Test2", "[a-z]+"));

        assertThat(grokPatternService.loadAll()).hasSize(2);
        facade.delete(grokPattern);
        assertThat(grokPatternService.loadAll()).hasSize(1);
    }

    @Test
    public void resolveEntityDescriptor() throws ValidationException {
        final GrokPattern grokPattern = grokPatternService.save(GrokPattern.create("Test1", "[a-z]+"));
        final EntityDescriptor descriptor = EntityDescriptor.create(grokPattern.id(), ModelTypes.GROK_PATTERN_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("grok-id"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(NullNode.getInstance())
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }

    @Test
    public void findExisting() throws ValidationException {
        final GrokPattern grokPattern = grokPatternService.save(GrokPattern.create("Test", "[a-z]+"));
        final Entity grokPatternEntity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(objectMapper.convertValue(GrokPatternEntity.create(
                        ValueReference.of("Test"),
                        ValueReference.of("[a-z]+")), JsonNode.class))
                .build();
        final Optional<NativeEntity<GrokPattern>> existingGrokPattern = facade.findExisting(grokPatternEntity, Collections.emptyMap());
        final NativeEntityDescriptor expectedDescriptor = NativeEntityDescriptor.create(grokPatternEntity.id(), "1", ModelTypes.GROK_PATTERN_V1);
        assertThat(existingGrokPattern)
                .isPresent()
                .get()
                .satisfies(nativeEntity -> {
                    assertThat(nativeEntity.descriptor()).isEqualTo(expectedDescriptor);
                    assertThat(nativeEntity.entity()).isEqualTo(grokPattern);
                });
    }

    @Test
    public void createNativeEntity() throws NotFoundException {
        final Entity grokPatternEntity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(objectMapper.convertValue(GrokPatternEntity.create(
                        ValueReference.of("Test"),
                        ValueReference.of("[a-z]+")), JsonNode.class))
                .build();
        final NativeEntity<GrokPattern> nativeEntity = facade.createNativeEntity(grokPatternEntity, Collections.emptyMap(), Collections.emptyMap(), "admin");

        final GrokPattern expectedGrokPattern = GrokPattern.create("1", "Test", "[a-z]+", null);
        final NativeEntityDescriptor expectedDescriptor = NativeEntityDescriptor.create("1", "1", ModelTypes.GROK_PATTERN_V1);

        assertThat(nativeEntity.descriptor()).isEqualTo(expectedDescriptor);
        assertThat(nativeEntity.entity()).isEqualTo(expectedGrokPattern);
        assertThat(grokPatternService.load("1")).isEqualTo(expectedGrokPattern);
    }

    @Test
    public void findExistingFailsWithDivergingPatterns() throws ValidationException {
        grokPatternService.save(GrokPattern.create("Test", "[a-z]+"));
        final Entity grokPatternEntity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(objectMapper.convertValue(GrokPatternEntity.create(
                        ValueReference.of("Test"),
                        ValueReference.of("BOOM")), JsonNode.class))
                .build();
        assertThatThrownBy(() -> facade.findExisting(grokPatternEntity, Collections.emptyMap()))
                .isInstanceOf(DivergingEntityConfigurationException.class)
                .hasMessage("Expected Grok pattern for name \"Test\": <BOOM>; actual Grok pattern: <[a-z]+>");
    }
}
