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
import javax.ws.rs.ext.ContextResolver;

@javax.ws.rs.ext.Provider
public class ObjectMapperProvider implements Provider<ObjectMapper>, ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public ObjectMapperProvider() {
        objectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(new PreserveLeadingUnderscoreStrategy())
                .registerModule(new JodaModule())
                .registerModule(new GuavaModule())
                .registerModule(new SimpleModule().addSerializer(new RangeJsonSerializer()));
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return get();
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }

    /**
     * This abomination is necessary because when using MongoJack to read "_id" object ids back from the database
     * the property name isn't correctly mapped to the POJO using the LowerCaseWithUnderscoresStrategy.
     * <p>
     * Apparently no one in the world tried to use a different naming strategy with MongoJack.
     * (one of my many useless talents is finding corner cases).
     * </p>
     */
    public class PreserveLeadingUnderscoreStrategy extends PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy {
        @Override
        public String translate(String input) {
            String translated = super.translate(input);
            if (input.startsWith("_") && !translated.startsWith("_")) {
                translated = "_" + translated; // lol underscore
            }
            return translated;
        }
    }
}
