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
package org.graylog2.contentpacks.jersey;

import org.graylog2.contentpacks.model.ModelId;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Singleton
public class ModelIdParamConverter implements ParamConverter<ModelId> {
    /**
     * {@inheritDoc}
     */
    @Override
    public ModelId fromString(final String value) {
        return ModelId.of(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(final ModelId value) {
        return value == null ? null : value.id();
    }

    public static class Provider implements ParamConverterProvider {
        private final ModelIdParamConverter paramConverter = new ModelIdParamConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        @Nullable
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
                                                  final Annotation[] annotations) {
            return ModelId.class.isAssignableFrom(rawType) ? (ParamConverter<T>) paramConverter : null;
        }
    }
}