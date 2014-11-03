/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Inject;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import javax.inject.Provider;

public class ServerObjectMapperProvider extends ObjectMapperProvider implements Provider<ObjectMapper> {
    private final ObjectMapper objectMapper;

    @Inject
    public ServerObjectMapperProvider(final ObjectMapperProvider objectMapperProvider) {
        this.objectMapper = objectMapperProvider.get()
                .copy()
                .registerModule(new SimpleModule().addSerializer(new ObjectIdSerializer()));
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }
}
