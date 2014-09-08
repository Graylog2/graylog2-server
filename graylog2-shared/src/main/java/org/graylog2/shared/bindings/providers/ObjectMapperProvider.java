/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
