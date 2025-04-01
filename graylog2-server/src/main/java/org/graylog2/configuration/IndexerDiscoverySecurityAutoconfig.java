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
        if (getRenewalPolicy().isEmpty()) {
            LOG.info("Setting renewal policy to " + DEFAULT_CERT_RENEWAL_POLICY);
            clusterConfigService.write(DEFAULT_CERT_RENEWAL_POLICY);
        }

        if (!caKeystore.exists()) {
            LOG.info("Creating new self-signed Graylog CA");
            caKeystore.createSelfSigned("Graylog CA");
        }
        LOG.info("Self-signed graylog CA configuration has been successfully initialized");
    }

    private Optional<RenewalPolicy> getRenewalPolicy() {
        return Optional.ofNullable(this.clusterConfigService.get(RenewalPolicy.class));
    }

    @Override
    public void beforeIndexerDiscovery() {
        if (configuration.selfsignedStartupEnabled()) {
            // We assume that this will run on a new cluster that doesn't contain any preflight config result value
            // so the upsert during setConfigResult will create a new value. This information is then used to decide
            // if this node should configure the rest of the selfsigned setup. Only if this is the first node, creating
            // the value, we will continue with the setup.
            // Ideally we'd use LeaderElectionService#isLeader for this decision. But this code runs earlier than
            // the service is started and initialized, so we need a workaround. It doesn't matter if this code
            // runs on a leader or follower node, as long as it runs only once.
            final PreflightConfigService.ConfigResultState writeResult = preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
            if (writeResult == PreflightConfigService.ConfigResultState.CREATED) {
                LOG.info("Self-signed security startup enabled, configuring renewal policy and CA");
                configureSelfsignedStartup();
            } else {
                LOG.info("Skipping Self-signed security startup, as it is already configured");
            }
        }
    }

    @Override
    public void onDiscoveryRetry() {

    }
}
