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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import javax.inject.Inject;

public class StreamConverter implements EntityConverter<Stream> {
    private final ObjectMapper objectMapper;

    @Inject
    public StreamConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(Stream stream) {
        final ObjectNode data = objectMapper.createObjectNode()
                .put("title", stream.getTitle())
                .put("description", stream.getDescription())
                .put("matching_type", stream.getMatchingType().name())
                .put("remove_matches_from_default_stream", stream.getRemoveMatchesFromDefaultStream());

        // TODO: How to handle output references?
        // stream.getOutputs()

        final ArrayNode streamRules = data.putArray("rules");
        for (StreamRule streamRule : stream.getStreamRules()) {
            final ObjectNode rule = objectMapper.createObjectNode()
                    .put("description", streamRule.getDescription())
                    .put("type", streamRule.getType().name())
                    .put("field", streamRule.getField())
                    .put("value", streamRule.getValue())
                    .put("inverted", streamRule.getInverted());
            streamRules.add(rule);
        }

        return EntityV1.builder()
                .id(ModelId.of(stream.getId()))
                .type(ModelTypes.STREAM)
                .data(data)
                .build();
    }
}
