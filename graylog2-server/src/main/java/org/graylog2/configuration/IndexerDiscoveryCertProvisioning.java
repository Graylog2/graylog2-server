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
package org.graylog2.configuration;

import jakarta.inject.Inject;
import org.graylog2.bootstrap.preflight.GraylogCertificateProvisioner;

public class IndexerDiscoveryCertProvisioning implements IndexerDiscoveryListener {

    private final GraylogCertificateProvisioner graylogCertificateProvisioner;

    @Inject
    public IndexerDiscoveryCertProvisioning(GraylogCertificateProvisioner graylogCertificateProvisioner) {
        this.graylogCertificateProvisioner = graylogCertificateProvisioner;
    }

    @Override
    public void beforeIndexerDiscovery() {

    }

    @Override
    public void onDiscoveryRetry() {
        // let's try to provision certificates, maybe there are datanodes waiting for these
        graylogCertificateProvisioner.runProvisioning();
    }
}
