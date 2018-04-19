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
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.graylog2.streams.OutputService;

import javax.inject.Inject;

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
    public Entity encode(Output output) {
        final OutputEntity outputEntity = OutputEntity.create(
                output.getTitle(),
                output.getType(),
                output.getConfiguration()
        );
        final JsonNode data = objectMapper.convertValue(outputEntity, JsonNode.class);

        return EntityV1.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT)
                .data(data)
                .build();
    }

    @Override
    public Output decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private Output decodeEntityV1(EntityV1 entity) {
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        final CreateOutputRequest createOutputRequest = CreateOutputRequest.create(
                outputEntity.title(),
                outputEntity.type(),
                outputEntity.configuration(),
                null // TODO
        );
        try {
            // TODO: Pass along user
            return outputService.create(createOutputRequest, "admin");
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
