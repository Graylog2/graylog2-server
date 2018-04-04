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
package org.graylog2.contentpacks.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.plugin.streams.Output;

import javax.inject.Inject;

public class OutputConverter implements EntityConverter<Output> {
    private final ObjectMapper objectMapper;

    @Inject
    public OutputConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(Output output) {
        final ObjectNode data = objectMapper.createObjectNode()
                .put("title", output.getTitle())
                .put("type", output.getType());

        // TODO: Create independent representation of entity?
        data.set("configuration", objectMapper.convertValue(output.getConfiguration(), JsonNode.class));

        return EntityV1.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT)
                .data(data)
                .build();
    }
}
