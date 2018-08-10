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
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GrokPatternFacade implements EntityFacade<GrokPattern> {
    private static final Logger LOG = LoggerFactory.getLogger(GrokPatternFacade.class);

    public static final ModelType TYPE = ModelTypes.GROK_PATTERN_V1;

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
                .id(ModelId.of(grokPattern.id()))
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
            return NativeEntity.create(entity.id(), savedGrokPattern.id(), TYPE, savedGrokPattern);
        } catch (ValidationException e) {
            throw new RuntimeException("Couldn't create grok pattern " + grokPattern.name());
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

        return grokPattern.map(gp -> NativeEntity.create(entity.id(), gp.id(), TYPE, gp));
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
}
