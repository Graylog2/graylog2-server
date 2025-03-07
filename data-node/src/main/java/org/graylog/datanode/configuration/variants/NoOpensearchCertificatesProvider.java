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
package org.graylog.datanode.configuration.variants;

import org.graylog.datanode.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class NoOpensearchCertificatesProvider implements OpensearchCertificatesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpensearchCertificatesProvider.class);

    @Override
    public boolean isConfigured(final Configuration localConfiguration) {
        return localConfiguration.isInsecureStartup();
    }

    @Override
    public OpensearchCertificates build() {
         LOG.warn("Insecure configuration is deprecated. Please use selfsigned_startup to create fully encrypted setups.");
        return OpensearchCertificates.none();
    }
}
