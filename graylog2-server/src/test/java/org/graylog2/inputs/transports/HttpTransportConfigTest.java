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
package org.graylog2.inputs.transports;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpTransportConfigTest {
    @Test
    public void testGetRequestedConfiguration() {
        HttpTransport.Config config = new HttpTransport.Config();

        final ConfigurationRequest requestedConfiguration = config.getRequestedConfiguration();
        assertTrue(requestedConfiguration.containsField(HttpTransport.CK_ENABLE_CORS));
        assertEquals(ConfigurationField.Optional.OPTIONAL, requestedConfiguration.getField(HttpTransport.CK_ENABLE_CORS).isOptional());
        assertEquals(true, requestedConfiguration.getField(HttpTransport.CK_ENABLE_CORS).getDefaultValue());

        assertTrue(requestedConfiguration.containsField(HttpTransport.CK_MAX_CHUNK_SIZE));
        assertEquals(ConfigurationField.Optional.OPTIONAL, requestedConfiguration.getField(HttpTransport.CK_MAX_CHUNK_SIZE).isOptional());
        assertEquals(65536, requestedConfiguration.getField(HttpTransport.CK_MAX_CHUNK_SIZE).getDefaultValue());
    }
}
