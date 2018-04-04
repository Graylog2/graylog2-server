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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.inputs.Extractor;

import javax.inject.Inject;
import java.util.List;

public class InputConverter implements EntityConverter<InputWithExtractors> {
    private final ObjectMapper objectMapper;

    @Inject
    public InputConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(InputWithExtractors inputWithExtractors) {
        final Input input = inputWithExtractors.input();
        final ObjectNode data = objectMapper.createObjectNode()
                .put("title", input.getTitle())
                .put("type", input.getType())
                .put("global", input.isGlobal());

        // TODO: Create independent representation of entity?
        data.set("static_fields", objectMapper.convertValue(input.getStaticFields(), JsonNode.class));
        data.set("configuration", objectMapper.convertValue(input.getConfiguration(), JsonNode.class));

        final List<Extractor> extractors = inputWithExtractors.extractors();
        final ArrayNode extractorsArray = objectMapper.createArrayNode();
        for (Extractor extractor : extractors) {
            extractorsArray.add(objectMapper.convertValue(extractor.getPersistedFields(), JsonNode.class));
        }
        data.set("extractors", extractorsArray);

        return EntityV1.builder()
                .id(ModelId.of(input.getId()))
                .type(ModelTypes.INPUT)
                .data(data)
                .build();
    }
}
