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

package org.graylog.grn;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Converts GRN strings format into a {@link GRN} object.
 */
public class GRNDeserializer extends StdDeserializer<GRN> {
    private final GRNRegistry grnRegistry;

    public GRNDeserializer(GRNRegistry grnRegistry) {
        super(GRN.class);
        this.grnRegistry = grnRegistry;
    }

    @Override
    public GRN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return grnRegistry.parse(p.getValueAsString());
    }
}

