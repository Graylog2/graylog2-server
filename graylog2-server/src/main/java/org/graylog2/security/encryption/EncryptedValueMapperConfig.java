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
package org.graylog2.security.encryption;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configures an {@link ObjectMapper} to enable database serialization for {@link EncryptedValue}.
 */
public class EncryptedValueMapperConfig {
    private static final String KEY = EncryptedValueMapperConfig.class.getCanonicalName();

    private enum Type {
        DATABASE
    }

    public static boolean isDatabase(DatabindContext ctx) {
        return Type.DATABASE.equals(ctx.getAttribute(KEY));
    }

    public static void enableDatabase(ObjectMapper objectMapper) {
        // The serializer and deserializer will switch modes depending on the attribute
        objectMapper
                .setConfig(objectMapper.getDeserializationConfig().withAttribute(KEY, Type.DATABASE))
                .setConfig(objectMapper.getSerializationConfig().withAttribute(KEY, Type.DATABASE));
    }
}
