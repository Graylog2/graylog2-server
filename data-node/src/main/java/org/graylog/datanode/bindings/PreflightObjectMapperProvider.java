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
package org.graylog.datanode.bindings;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.grn.GRNRegistry;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.GraylogClassLoader;

import java.util.Collections;

/**
 * This ObjectMapperProvider should be used only for preflight checks and preflight web. It's significantly limited.
 * For all other usages, please refer to {@link ObjectMapperProvider}.
 */
public class PreflightObjectMapperProvider implements Provider<ObjectMapper> {

    private final ObjectMapperProvider delegate;

    @Inject
    public PreflightObjectMapperProvider(@GraylogClassLoader ClassLoader classLoader, EncryptedValueService encryptedValueService) {
        delegate = new ObjectMapperProvider(
                classLoader,
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        );
    }

    @Override
    public ObjectMapper get() {
        return delegate.get();
    }
}
