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
import org.graylog.security.certutil.CaKeystore;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class IndexerDiscoverySecurityAutoconfig implements IndexerDiscoveryListener {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerDiscoverySecurityAutoconfig.class);
    private static final RenewalPolicy DEFAULT_CERT_RENEWAL_POLICY = new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, "P30D");

    private final ClusterConfigService clusterConfigService;
    private final CaKeystore caKeystore;
    private final Configuration configuration;
    private final PreflightConfigService preflightConfigService;

    @Inject
    public IndexerDiscoverySecurityAutoconfig(Configuration configuration, PreflightConfigService preflightConfigService, ClusterConfigService clusterConfigService, CaKeystore caKeystore) {
        this.configuration = configuration;
        this.preflightConfigService = preflightConfigService;
        this.clusterConfigService = clusterConfigService;
        this.caKeystore = caKeystore;
    }

    public void configureSelfsignedStartup() {
        final PreflightConfigResult preflightConfigResult = preflightConfigService.getPreflightConfigResult();
        if (preflightConfigResult == PreflightConfigResult.UNKNOWN) {
            LOG.info("Setting preflight config to FINISHED");
            preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
        }

        if (getRenewalPolicy().isEmpty()) {
            LOG.info("Setting renewal policy to " + DEFAULT_CERT_RENEWAL_POLICY);
            clusterConfigService.write(DEFAULT_CERT_RENEWAL_POLICY);
        }

        if (!caKeystore.exists()) {
            LOG.info("Creating new self-signed Graylog CA");
            caKeystore.createSelfSigned("Graylog CA");
        }
    }

    private Optional<RenewalPolicy> getRenewalPolicy() {
        return Optional.ofNullable(this.clusterConfigService.get(RenewalPolicy.class));
    }

    @Override
    public void beforeIndexerDiscovery() {
        if (configuration.selfsignedStartupEnabled()) {
            LOG.info("Self-signed security startup enabled, configuring renewal policy and CA if needed");
            configureSelfsignedStartup();
        }
    }

    @Override
    public void onDiscoveryRetry() {

    }
}
