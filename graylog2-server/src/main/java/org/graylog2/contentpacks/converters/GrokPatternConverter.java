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
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.grok.GrokPattern;

import javax.inject.Inject;

public class GrokPatternConverter implements EntityConverter<GrokPattern> {
    private final ObjectMapper objectMapper;

    @Inject
    public GrokPatternConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(GrokPattern grokPattern) {
        final JsonNode data = objectMapper.createObjectNode()
                .put("name", grokPattern.name())
                .put("pattern", grokPattern.pattern());

        return EntityV1.builder()
                .id(ModelId.of(grokPattern.id()))
                .type(ModelTypes.GROK_PATTERN)
                .data(data)
                .build();
    }
}
