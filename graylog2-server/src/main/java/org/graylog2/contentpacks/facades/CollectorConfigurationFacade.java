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
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.CollectorConfigurationEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class CollectorConfigurationFacade implements EntityFacade<Configuration> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorConfigurationFacade.class);

    public static final ModelType TYPE = ModelTypes.COLLECTOR_CONFIGURATION;

    private final ObjectMapper objectMapper;
    private final ConfigurationService configurationService;

    @Inject
    public CollectorConfigurationFacade(ObjectMapper objectMapper, ConfigurationService configurationService) {
        this.objectMapper = objectMapper;
        this.configurationService = configurationService;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(Configuration configuration) {
        final CollectorConfigurationEntity configurationEntity = CollectorConfigurationEntity.create(
                ValueReference.of(configuration.collectorId()),
                ValueReference.of(configuration.name()),
                ValueReference.of(configuration.color()),
                ValueReference.of(configuration.template()));
        final JsonNode data = objectMapper.convertValue(configurationEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(configuration.id()))
                .type(TYPE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public NativeEntity<Configuration> createNativeEntity(Entity entity,
                                                          Map<String, ValueReference> parameters,
                                                          Map<EntityDescriptor, Object> nativeEntities,
                                                          String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<Configuration> decode(EntityV1 entity, Map<String, ValueReference> parameters) {
        final CollectorConfigurationEntity configurationEntity = objectMapper.convertValue(entity.data(), CollectorConfigurationEntity.class);
        final Configuration configuration = Configuration.create(
                configurationEntity.collectorId().asString(parameters),
                configurationEntity.title().asString(parameters),
                configurationEntity.color().asString(parameters),
                configurationEntity.template().asString(parameters));

        final Configuration savedConfiguration = configurationService.save(configuration);
        return NativeEntity.create(savedConfiguration.id(), TYPE, savedConfiguration);
    }

    @Override
    public Optional<NativeEntity<Configuration>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        // TODO: Check if multiple configurations are allowed to exist (bernd)
        return Optional.empty();
    }

    @Override
    public void delete(Configuration nativeEntity) {
        configurationService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(Configuration configuration) {
        return EntityExcerpt.builder()
                .id(ModelId.of(configuration.id()))
                .type(TYPE)
                .title(configuration.name())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return configurationService.all().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        final Configuration configuration = configurationService.find(modelId.id());
        if (isNull(configuration)) {
            LOG.debug("Couldn't find collector configuration {}", entityDescriptor);
            return Optional.empty();
        }

        return Optional.of(exportNativeEntity(configuration));
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Configuration configuration = configurationService.find(modelId.id());
        if (isNull(configuration)) {
            LOG.debug("Could not find configuration {}", entityDescriptor);
        } else {
            final EntityDescriptor collectorEntityDescriptor = EntityDescriptor.create(
                    configuration.collectorId(), ModelTypes.COLLECTOR);
            mutableGraph.putEdge(entityDescriptor, collectorEntityDescriptor);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveEntityV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<String, ValueReference> parameters,
                                          Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final CollectorConfigurationEntity configurationEntity = objectMapper.convertValue(entity.data(), CollectorConfigurationEntity.class);
        final EntityDescriptor collectorDescriptor = EntityDescriptor.create(configurationEntity.collectorId().asString(parameters), ModelTypes.COLLECTOR);
        final Entity collectorEntity = entities.get(collectorDescriptor);

        if (collectorEntity != null) {
            mutableGraph.putEdge(entity, collectorEntity);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
