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
package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.graylog2.shared.rest.RangeJsonSerializer;

import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    private final SimpleModule simpleModule;

    public ObjectMapperProvider() {
        simpleModule = new SimpleModule() {
            {
                addSerializer(new RangeJsonSerializer());
            }
        };
    }

    @Override
    public ObjectMapper get() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
