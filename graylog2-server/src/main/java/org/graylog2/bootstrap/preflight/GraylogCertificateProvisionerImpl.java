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
package org.graylog2.bootstrap.preflight;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.security.certutil.CaKeystore;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class GraylogCertificateProvisionerImpl implements GraylogCertificateProvisioner {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertificateProvisionerImpl.class);

    private final CaKeystore caKeystore;
    private final ClusterConfigService clusterConfigService;

    private final CertificateExchange certificateExchange;

    @Inject
    public GraylogCertificateProvisionerImpl(CaKeystore caKeystore, ClusterConfigService clusterConfigService, CertificateExchange certificateExchange) {
        this.caKeystore = caKeystore;
        this.clusterConfigService = clusterConfigService;
        this.certificateExchange = certificateExchange;
    }


    public void runProvisioning() {
        try {
            if (!caKeystore.exists()) {
                LOG.debug("No CA keystore available.");
                return;
            }

            final var renewalPolicy = getRenewalPolicy();
            if (renewalPolicy == null) {
                LOG.debug("No renewal policy available.");
                return;
            }

            certificateExchange.signPendingCertificateRequests(request -> caKeystore.signCertificateRequest(request, renewalPolicy));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RenewalPolicy getRenewalPolicy() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }
}
