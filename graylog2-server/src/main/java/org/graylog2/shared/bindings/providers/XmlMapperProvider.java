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

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.grn.GRNRegistry;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;
import org.graylog2.shared.plugins.GraylogClassLoader;

import java.util.Set;

public class XmlMapperProvider implements Provider<XmlMapper> {

    private final XmlMapper objectMapper;

    @Inject
    public XmlMapperProvider(@GraylogClassLoader final ClassLoader classLoader,
                             @JacksonSubTypes final Set<NamedType> subtypes,
                             final EncryptedValueService encryptedValueService,
                             final GRNRegistry grnRegistry,
                             final InputConfigurationBeanDeserializerModifier inputConfigurationBeanDeserializerModifier) {
        final XmlMapper mapper = new XmlMapper.Builder(new XmlMapper())
                .defaultUseWrapper(true)
                .build();

        this.objectMapper = ObjectMapperConfiguration.configureMapper(mapper,
                classLoader,
                subtypes,
                encryptedValueService,
                grnRegistry,
                inputConfigurationBeanDeserializerModifier);

    }

    @Override
    public XmlMapper get() {
        return objectMapper;
    }
}
