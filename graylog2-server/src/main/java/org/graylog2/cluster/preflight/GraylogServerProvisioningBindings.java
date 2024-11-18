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
package org.graylog2.cluster.preflight;

import com.google.inject.AbstractModule;
import org.graylog2.bootstrap.preflight.GraylogCertificateProvisioner;
import org.graylog2.bootstrap.preflight.GraylogCertificateProvisionerImpl;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateExchangeImpl;
import org.graylog2.configuration.IndexerDiscoverySecurityAutoconfig;

public class GraylogServerProvisioningBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(CertificateExchange.class).to(CertificateExchangeImpl.class);
        bind(GraylogCertificateProvisioner.class).to(GraylogCertificateProvisionerImpl.class);
        bind(IndexerDiscoverySecurityAutoconfig.class);
    }
}
