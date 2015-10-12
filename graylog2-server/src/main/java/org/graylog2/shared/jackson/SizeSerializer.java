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
package org.graylog2.shared.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.joschi.jadconfig.util.Size;

import java.io.IOException;

/**
 * Serializes JadConfig's Size utility object to bytes.
 */
public class SizeSerializer extends JsonSerializer<Size> {
    @Override
    public Class<Size> handledType() {
        return Size.class;
    }

    @Override
    public void serialize(Size value,
                          JsonGenerator jgen,
                          SerializerProvider provider) throws IOException {
        jgen.writeNumber(value.toBytes());
    }
}
