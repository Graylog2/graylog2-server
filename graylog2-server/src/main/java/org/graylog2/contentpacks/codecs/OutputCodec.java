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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.graylog2.streams.OutputService;

import javax.inject.Inject;
import java.util.Map;

import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class OutputCodec implements EntityCodec<Output> {
    private final ObjectMapper objectMapper;
    private final OutputService outputService;

    @Inject
    public OutputCodec(ObjectMapper objectMapper,
                       OutputService outputService) {
        this.objectMapper = objectMapper;
        this.outputService = outputService;
    }

    @Override
    public EntityWithConstraints encode(Output output) {
        final OutputEntity outputEntity = OutputEntity.create(
                ValueReference.of(output.getTitle()),
                ValueReference.of(output.getType()),
                toReferenceMap(output.getConfiguration())
        );
        final JsonNode data = objectMapper.convertValue(outputEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public Output decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private Output decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters, String username) {
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        final CreateOutputRequest createOutputRequest = CreateOutputRequest.create(
                outputEntity.title().asString(parameters),
                outputEntity.type().asString(parameters),
                toValueMap(outputEntity.configuration(), parameters),
                null // TODO
        );
        try {
            return outputService.create(createOutputRequest, username);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public EntityExcerpt createExcerpt(Output output) {
        return EntityExcerpt.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT)
                .title(output.getTitle())
                .build();
    }
}
