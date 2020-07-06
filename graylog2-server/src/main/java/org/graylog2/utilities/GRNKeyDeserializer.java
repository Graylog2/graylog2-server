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

package org.graylog2.utilities;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

// TODO Not sure why this is needed
public class GRNKeyDeserializer extends KeyDeserializer {
    private final GRNRegistry grnRegistry;

    public GRNKeyDeserializer(GRNRegistry grnRegistry) {
        super();
        this.grnRegistry = grnRegistry;
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return grnRegistry.parse(key);
    }

}

