/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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