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
package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;

public class XmlMapperProvider implements Provider<XmlMapper> {

    private final XmlMapper objectMapper;

    @Inject
    public XmlMapperProvider(ObjectMapperConfiguration configuration) {
        final XmlMapper mapper = new XmlMapper.Builder(new XmlMapper())
                .defaultUseWrapper(true)
                .build();

        this.objectMapper = configuration.configure(mapper);

    }

    @Override
    public XmlMapper get() {
        return objectMapper;
    }
}
