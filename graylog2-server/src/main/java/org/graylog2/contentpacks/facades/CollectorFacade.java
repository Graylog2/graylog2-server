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
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.CollectorEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class CollectorFacade implements EntityFacade<Collector> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorFacade.class);

    public static final ModelType TYPE = ModelTypes.COLLECTOR;

    private final ObjectMapper objectMapper;
    private final CollectorService collectorService;

    @Inject
    public CollectorFacade(ObjectMapper objectMapper, CollectorService collectorService) {
        this.objectMapper = objectMapper;
        this.collectorService = collectorService;
    }

    @Override
    public EntityWithConstraints encode(Collector collector) {
        final List<ValueReference> executeParameters = collector.executeParameters().stream()
                .map(ValueReference::of)
                .collect(Collectors.toList());
        final List<ValueReference> validationCommand = collector.validationCommand().stream()
                .map(ValueReference::of)
                .collect(Collectors.toList());

        final CollectorEntity collectorEntity = CollectorEntity.create(
                ValueReference.of(collector.name()),
                ValueReference.of(collector.serviceType()),
                ValueReference.of(collector.nodeOperatingSystem()),
                ValueReference.of(collector.executablePath()),
                ValueReference.of(collector.configurationPath()),
                executeParameters,
                validationCommand,
                ValueReference.of(collector.defaultTemplate())
        );

        final JsonNode data = objectMapper.convertValue(collectorEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(collector.id()))
                .type(TYPE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public Collector decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Collector decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters) {
        final CollectorEntity collectorEntity = objectMapper.convertValue(entity.data(), CollectorEntity.class);

        final List<String> executeParameters = collectorEntity.executeParameters().stream()
                .map(parameter -> parameter.asString(parameters))
                .collect(Collectors.toList());
        final List<String> validationCommand = collectorEntity.validationCommand().stream()
                .map(parameter -> parameter.asString(parameters))
                .collect(Collectors.toList());

        final Collector collector = Collector.builder()
                .name(collectorEntity.title().asString(parameters))
                .serviceType(collectorEntity.serviceType().asString(parameters))
                .nodeOperatingSystem(collectorEntity.nodeOperatingSystem().asString(parameters))
                .executablePath(collectorEntity.executablePath().asString(parameters))
                .configurationPath(collectorEntity.configurationPath().asString(parameters))
                .executeParameters(executeParameters)
                .validationCommand(validationCommand)
                .defaultTemplate(collectorEntity.defaultTemplate().asString(parameters))
                .build();

        return collectorService.save(collector);
    }

    @Override
    public EntityExcerpt createExcerpt(Collector collector) {
        return EntityExcerpt.builder()
                .id(ModelId.of(collector.id()))
                .type(TYPE)
                .title(collector.name())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return collectorService.all().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        final Collector collector = collectorService.find(modelId.id());
        if (isNull(collector)) {
            LOG.debug("Couldn't find collector {}", entityDescriptor);
            return Optional.empty();
        }

        return Optional.of(encode(collector));
    }
}
