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
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import io.krakens.grok.api.GrokUtils;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
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
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class GrokPatternFacade implements EntityFacade<GrokPattern> {
    private static final Logger LOG = LoggerFactory.getLogger(GrokPatternFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.GROK_PATTERN_V1;

    private final ObjectMapper objectMapper;
    private final GrokPatternService grokPatternService;

    @Inject
    public GrokPatternFacade(ObjectMapper objectMapper, GrokPatternService grokPatternService) {
        this.objectMapper = objectMapper;
        this.grokPatternService = grokPatternService;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(GrokPattern grokPattern) {
        final GrokPatternEntity grokPatternEntity = GrokPatternEntity.create(
                ValueReference.of(grokPattern.name()),
                ValueReference.of(grokPattern.pattern()));
        final JsonNode data = objectMapper.convertValue(grokPatternEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public NativeEntity<GrokPattern> createNativeEntity(Entity entity,
                                                        Map<String, ValueReference> parameters,
                                                        Map<EntityDescriptor, Object> nativeEntities,
                                                        String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<GrokPattern> decode(EntityV1 entity, Map<String, ValueReference> parameters) {
        final GrokPatternEntity grokPatternEntity = objectMapper.convertValue(entity.data(), GrokPatternEntity.class);
        final String name = grokPatternEntity.name().asString(parameters);
        final String pattern = grokPatternEntity.pattern().asString(parameters);

        final GrokPattern grokPattern = GrokPattern.create(name, pattern);
        try {
            final GrokPattern savedGrokPattern = grokPatternService.save(grokPattern);
            return NativeEntity.create(entity.id(), savedGrokPattern.id(), TYPE_V1, savedGrokPattern.name(), savedGrokPattern);
        } catch (ValidationException e) {
            throw new RuntimeException("Couldn't create grok pattern " + grokPattern.name());
        }
    }

    @Override
    public Optional<NativeEntity<GrokPattern>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        try {
            final GrokPattern grokPattern = grokPatternService.load(nativeEntityDescriptor.id().id());
            return Optional.of(NativeEntity.create(nativeEntityDescriptor, grokPattern));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(GrokPattern nativeEntity) {
        grokPatternService.delete(nativeEntity.id());
    }

    @Override
    public Optional<NativeEntity<GrokPattern>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<GrokPattern>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final GrokPatternEntity grokPatternEntity = objectMapper.convertValue(entity.data(), GrokPatternEntity.class);
        final String name = grokPatternEntity.name().asString(parameters);
        final String pattern = grokPatternEntity.pattern().asString(parameters);

        final Optional<GrokPattern> grokPattern = grokPatternService.loadByName(name);
        grokPattern.ifPresent(existingPattern -> compareGrokPatterns(name, pattern, existingPattern.pattern()));

        return grokPattern.map(gp -> NativeEntity.create(entity.id(), gp.id(), TYPE_V1,gp.name(), gp));
    }

    private void compareGrokPatterns(String name, String expectedPattern, String actualPattern) {
        if (!actualPattern.equals(expectedPattern)) {
            throw new DivergingEntityConfigurationException("Expected Grok pattern for name \"" + name + "\": <" + expectedPattern + ">; actual Grok pattern: <" + actualPattern + ">");
        }
    }

    @Override
    public EntityExcerpt createExcerpt(GrokPattern grokPattern) {
        return EntityExcerpt.builder()
                .id(ModelId.of(grokPattern.id()))
                .type(ModelTypes.GROK_PATTERN_V1)
                .title(grokPattern.name())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return grokPatternService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final GrokPattern grokPattern = grokPatternService.load(modelId.id());
            return Optional.of(exportNativeEntity(grokPattern));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find grok pattern {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        try {
            final GrokPattern grokPattern = grokPatternService.load(modelId.id());

            final Set<String> namedGroups = GrokUtils.getNameGroups(GrokUtils.GROK_PATTERN.pattern());
            final String namedPattern = grokPattern.pattern();
            final Matcher matcher = GrokUtils.GROK_PATTERN.matcher(namedPattern);
            while (matcher.find()) {
                final Map<String, String> group = GrokUtils.namedGroups(matcher, namedGroups);
                final String patternName = group.get("pattern");
                grokPatternService.loadByName(patternName).ifPresent(depPattern -> {
                    final EntityDescriptor depEntityDescriptor = EntityDescriptor.create(
                            depPattern.id(), ModelTypes.GROK_PATTERN_V1);
                    mutableGraph.putEdge(entityDescriptor, depEntityDescriptor);
                });
            }
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find grok pattern {}", entityDescriptor, e);
        }
        return mutableGraph;
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveForInstallationV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallationV1(EntityV1 entity,
                                                   Map<String, ValueReference> parameters,
                                                   Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final GrokPatternEntity grokPatternEntity = objectMapper.convertValue(entity.data(), GrokPatternEntity.class);

        final Set<String> namedGroups = GrokUtils.getNameGroups(GrokUtils.GROK_PATTERN.pattern());
        final String namedPattern = grokPatternEntity.pattern().asString(parameters);
        final Matcher matcher = GrokUtils.GROK_PATTERN.matcher(namedPattern);
        while (matcher.find()) {
            final Map<String, String> group = GrokUtils.namedGroups(matcher, namedGroups);
            final String patternName = group.get("pattern");

            entities.entrySet().stream()
                    .filter(x -> x.getValue().type().equals(ModelTypes.GROK_PATTERN_V1))
                    .filter(x -> {
                        EntityV1 entityV1 = (EntityV1) x.getValue();
                        GrokPatternEntity grokPatternEntity1 = objectMapper.convertValue(entityV1.data(), GrokPatternEntity.class);
                        return grokPatternEntity1.name().asString(parameters).equals(patternName);
                    }).forEach(x -> mutableGraph.putEdge(entity, x.getValue()));
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
