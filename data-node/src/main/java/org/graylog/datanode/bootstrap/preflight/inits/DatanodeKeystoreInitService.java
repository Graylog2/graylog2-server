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
package org.graylog.datanode.bootstrap.preflight.inits;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.inject.Inject;
import org.graylog.datanode.bootstrap.preflight.DatanodeCertReceiver;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.datanode.opensearch.CsrRequester;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This init verifies that each datanode has a private key configured. It may be needed right now, but at the moment
 * we'll start provisioning and create certificate signing requests, we may be sure that there is one private key
 * available.
 */
public class DatanodeKeystoreInitService implements DatanodeBlockingInit {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystoreInitService.class);

    private final DatanodeKeystore datanodeKeystore;
    private final CsrRequester csrRequester;
    private final DatanodeCertReceiver datanodeCertReceiver;

    private final ClusterConfigService clusterConfigService;

    @Inject
    public DatanodeKeystoreInitService(
            DatanodeKeystore datanodeKeystore,
            CsrRequester csrRequester,
            DatanodeCertReceiver datanodeCertReceiver, ClusterConfigService clusterConfigService

    ) {
        this.datanodeKeystore = datanodeKeystore;
        this.csrRequester = csrRequester;
        this.datanodeCertReceiver = datanodeCertReceiver;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void runInit() {
        if (!datanodeKeystore.exists()) {
            LOG.info("Creating keystore for this data node");
            try {
                datanodeKeystore.initWithSelfSignedCertificate();
            } catch (DatanodeKeystoreException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("Private key for this data node already exists, skipping creation.");
            checkCertificateRenewal();
        }
    }

    private void checkCertificateRenewal() {
        try {
            if (isAutomaticRenewal() && datanodeKeystore.hasSignedCertificate()) {
                final Date expiration = datanodeKeystore.getCertificateExpiration();
                final Date now = new Date();
                final boolean expired = now.after(expiration);
                if (expired) {
                    LOG.info("Datanode certificate expired on {}. Requesting and awaiting new certificate", expiration);
                    csrRequester.triggerCertificateSigningRequest();
                    waitForCertificateRenewal();
                }
            }
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAutomaticRenewal() {
        return Optional.ofNullable(this.clusterConfigService.get(RenewalPolicy.class))
                .map(RenewalPolicy::mode)
                .filter(RenewalPolicy.Mode.AUTOMATIC::equals)
                .isPresent();
    }

    private void waitForCertificateRenewal() {
        try {
            RetryerBuilder.<Date>newBuilder()
                    .retryIfResult(expiration -> {
                        final Date now = new Date();
                        return now.after(expiration);
                    })
                    .withStopStrategy(StopStrategies.neverStop())
                    .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            LOG.info("Waiting for datanode certificate renewal, retry #{}", attempt.getAttemptNumber());
                            datanodeCertReceiver.pollCertificate();
                        }
                    })
                    .build()
                    .call(datanodeKeystore::getCertificateExpiration);
        } catch (ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }
}
