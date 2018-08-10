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
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.graylog2.streams.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class OutputFacade implements EntityFacade<Output> {
    private static final Logger LOG = LoggerFactory.getLogger(OutputFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.OUTPUT_V1;

    private final ObjectMapper objectMapper;
    private final OutputService outputService;
    private final Set<PluginMetaData> pluginMetaData;
    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> outputFactories;

    @Inject
    public OutputFacade(ObjectMapper objectMapper,
                        OutputService outputService,
                        Set<PluginMetaData> pluginMetaData,
                        Map<String, MessageOutput.Factory<? extends MessageOutput>> outputFactories) {
        this.objectMapper = objectMapper;
        this.outputService = outputService;
        this.pluginMetaData = pluginMetaData;
        this.outputFactories = outputFactories;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(Output output) {
        final OutputEntity outputEntity = OutputEntity.create(
                ValueReference.of(output.getTitle()),
                ValueReference.of(output.getType()),
                toReferenceMap(output.getConfiguration())
        );
        final JsonNode data = objectMapper.convertValue(outputEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT_V1)
                .data(data)
                .build();
        final Set<Constraint> constraints = versionConstraints(output);

        return EntityWithConstraints.create(entity, constraints);
    }

    private Set<Constraint> versionConstraints(Output output) {
        // TODO: Find more robust method of identifying the providing plugin
        final MessageOutput.Factory<? extends MessageOutput> outputFactory = outputFactories.get(output.getType());
        if (outputFactory == null) {
            throw new ContentPackException("Unknown output type: " + output.getType());
        }
        // We have to use the descriptor because the factory is only a runtime-generated proxy. :(
        final String packageName = outputFactory.getDescriptor().getClass().getPackage().getName();
        return pluginMetaData.stream()
                .filter(metaData -> packageName.startsWith(metaData.getClass().getPackage().getName()))
                .map(PluginVersionConstraint::of)
                .collect(Collectors.toSet());
    }

    @Override
    public NativeEntity<Output> createNativeEntity(Entity entity,
                                                   Map<String, ValueReference> parameters,
                                                   Map<EntityDescriptor, Object> nativeEntities,
                                                   String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private NativeEntity<Output> decode(EntityV1 entity, Map<String, ValueReference> parameters, String username) {
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        final CreateOutputRequest createOutputRequest = CreateOutputRequest.create(
                outputEntity.title().asString(parameters),
                outputEntity.type().asString(parameters),
                toValueMap(outputEntity.configuration(), parameters),
                null // Outputs are assigned to streams in StreamFacade
        );
        try {
            final Output output = outputService.create(createOutputRequest, username);
            return NativeEntity.create(entity.id(), output.getId(), TYPE_V1, output);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void delete(Output nativeEntity) {
        try {
            outputService.destroy(nativeEntity);
        } catch (NotFoundException ignore) {
        }
    }

    @Override
    public EntityExcerpt createExcerpt(Output output) {
        return EntityExcerpt.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT_V1)
                .title(output.getTitle())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return outputService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Output output = outputService.load(modelId.id());
            return Optional.of(exportNativeEntity(output));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find output {}", entityDescriptor, e);
            return Optional.empty();
        }
    }
}
