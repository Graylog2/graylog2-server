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

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.graylog.security.certutil.CaConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.cert.storage.CertChainMongoStorage;
import org.graylog.security.certutil.cert.storage.CertChainStorage;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog2.Configuration;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton
public class GraylogCertificateProvisioningPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertificateProvisioningPeriodical.class);
    private static final int THREADPOOL_THREADS = 5;
    private static final int CONNECTION_ATTEMPTS = 40;
    private static final int WAIT_BETWEEN_CONNECTION_ATTEMPTS = 3;
    private static final int RATIO_WHEN_WE_START_SHOWING_EXCEPTIONS = 2;

    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NodeService nodeService;

    private final CaConfiguration configuration;
    private final CsrMongoStorage csrStorage;
    private final CertChainStorage certMongoStorage;
    private final CaService caService;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;
    private final String passwordSecret;
    private final EventBus serverEventBus;
    private Optional<OkHttpClient> okHttpClient = Optional.empty();
    private final PreflightConfigService preflightConfigService;
    private final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider;
    private final NotificationService notificationService;
    private final ExecutorService executor;

    @Inject
    public GraylogCertificateProvisioningPeriodical(final DataNodeProvisioningService dataNodeProvisioningService,
                                                    final CsrMongoStorage csrStorage,
                                                    final CertChainMongoStorage certMongoStorage,
                                                    final CaService caService,
                                                    final Configuration configuration,
                                                    final NodeService nodeService,
                                                    final CsrSigner csrSigner,
                                                    final ClusterConfigService clusterConfigService,
                                                    final @Named("password_secret") String passwordSecret,
                                                    final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider,
                                                    final PreflightConfigService preflightConfigService,
                                                    final EventBus serverEventBus,
                                                    final NotificationService notificationService) {
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.csrStorage = csrStorage;
        this.certMongoStorage = certMongoStorage;
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.configuration = configuration;
        this.nodeService = nodeService;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
        this.serverEventBus = serverEventBus;
        this.preflightConfigService = preflightConfigService;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
        this.notificationService = notificationService;
        this.executor = Executors.newFixedThreadPool(THREADPOOL_THREADS, new ThreadFactoryBuilder().setNameFormat("provisioning-connectivity-check-task").build());
    }

    // building a httpclient to check the connectivity to OpenSearch - TODO: maybe replace it with a VersionProbe already?
    private Optional<OkHttpClient> buildConnectivityCheckOkHttpClient() {
        try {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            try {
                var sslContext = SSLContext.getInstance("TLS");
                var tm = new CustomCAX509TrustManager(caService, serverEventBus);
                sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());
                clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), tm);
            } catch (NoSuchAlgorithmException ex) {
                LOG.error("Could not set Graylog CA trustmanager: {}", ex.getMessage(), ex);
            }
            return Optional.of(clientBuilder.build());
        } catch (Exception ex) {
            LOG.error("Could not create temporary okhttpclient " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    private RenewalPolicy getRenewalPolicy() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    @Override
    public void doRun() {
        LOG.debug("checking if there are configuration steps to take care of");

        try {
            // only load nodes that are in a state that need sth done
            final var nodes = dataNodeProvisioningService.findAllNodesThatNeedAttention();
            if(!nodes.isEmpty()) {

                final var password = configuration.configuredCaExists()
                        ? configuration.getCaPassword().toCharArray()
                        : passwordSecret.toCharArray();
                Optional<KeyStore> optKey = caService.loadKeyStore();
                if (optKey.isEmpty()) {
                    LOG.debug("No keystore available.");
                    return;
                }

                final var renewalPolicy = getRenewalPolicy();
                if (renewalPolicy == null) {
                    LOG.debug("No renewal policy available.");
                    return;
                }

                if (okHttpClient.isEmpty()) {
                    okHttpClient = buildConnectivityCheckOkHttpClient();
                }

                var nodesByState = nodes.stream().collect(Collectors.groupingBy(node -> Optional.ofNullable(node.state())
                        .orElse(DataNodeProvisioningConfig.State.UNCONFIGURED)));

                // if we're running in post-preflight and new datanodes arrive, they should configure themselves automatically
                var cfg = preflightConfigService.getPreflightConfigResult();
                if (cfg.equals(PreflightConfigResult.FINISHED)) {
                    var unconfiguredNodes = nodesByState.get(DataNodeProvisioningConfig.State.UNCONFIGURED);
                    if (renewalPolicy.mode().equals(RenewalPolicy.Mode.AUTOMATIC)) {
                        unconfiguredNodes.forEach(c -> dataNodeProvisioningService.save(c.toBuilder()
                                .state(DataNodeProvisioningConfig.State.CONFIGURED)
                                .build()));
                    } else {
                        var hasUnconfiguredNodes = !unconfiguredNodes.isEmpty();
                        if (hasUnconfiguredNodes) {
                            var notification = notificationService.buildNow()
                                    .addType(Notification.Type.DATA_NODE_NEEDS_PROVISIONING)
                                    .addSeverity(Notification.Severity.URGENT);
                            notificationService.publishIfFirst(notification);
                        } else {
                            notificationService.fixed(Notification.Type.DATA_NODE_NEEDS_PROVISIONING);
                        }
                    }
                }

                final var caKeystore = optKey.get();
                final var nodesWithCSR = nodesByState.get(DataNodeProvisioningConfig.State.CSR);
                final var hasNodesWithCSR = !nodesWithCSR.isEmpty();
                if (hasNodesWithCSR) {
                    var caPrivateKey = (PrivateKey) caKeystore.getKey("ca", password);
                    var caCertificate = (X509Certificate) caKeystore.getCertificate("ca");
                    nodesWithCSR.forEach(c -> {
                        try {
                            var csr = csrStorage.readCsr(c.nodeId());
                            if (csr.isEmpty()) {
                                LOG.error("Node in CSR state, but no CSR present : " + c.nodeId());
                                dataNodeProvisioningService.save(c.toBuilder()
                                        .state(DataNodeProvisioningConfig.State.ERROR)
                                        .errorMsg("Node in CSR state, but no CSR present")
                                        .build());
                            } else {
                                var cert = csrSigner.sign(caPrivateKey, caCertificate, csr.get(), renewalPolicy);
                                //TODO: assumptions about the chain, to contain 2 CAs, named "ca" and "root"...
                                final List<X509Certificate> caCertificates = List.of(caCertificate);
                                certMongoStorage.writeCertChain(new CertificateChain(cert, caCertificates), c.nodeId());
                            }
                        } catch (Exception e) {
                            LOG.error("Could not sign CSR: " + e.getMessage(), e);
                            dataNodeProvisioningService.save(c.toBuilder().state(DataNodeProvisioningConfig.State.ERROR).errorMsg(e.getMessage()).build());
                        }
                    });
                }

                nodesByState.get(DataNodeProvisioningConfig.State.STORED)
                        .forEach(c -> {
                            dataNodeProvisioningService.save(c.toBuilder().state(DataNodeProvisioningConfig.State.CONNECTING).build());
                            executor.submit(() -> {
                                try {
                                    checkConnectivity(c);
                                } catch (ExecutionException | RetryException e) {
                                    LOG.error("Exception trying to connect to node " + c.nodeId() + ": " + e.getMessage(), e);
                                    dataNodeProvisioningService.save(c.toBuilder().state(DataNodeProvisioningConfig.State.ERROR).errorMsg(e.getMessage()).build());
                                }
                            });
                        });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkConnectivity(final DataNodeProvisioningConfig config) throws ExecutionException, RetryException {
        LOG.info("Starting connectivity check with node {}", config.nodeId());
        final var counter = new AtomicInteger(0);
        final var nodeId = config.nodeId();
        RetryerBuilder.<String>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(WAIT_BETWEEN_CONNECTION_ATTEMPTS, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(CONNECTION_ATTEMPTS))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        LOG.debug("Waiting for datanode {} to come up, attempt {}", config.nodeId(), attempt.getAttemptNumber());
                        counter.incrementAndGet();
                    }
                })
                .retryIfResult(check -> Objects.equals("false", check))
                .build()
                .call(() -> {
                    try {
                        final var node = nodeService.byNodeId(nodeId);
                        Request request = new Request.Builder().url(node.getTransportAddress()).build();
                        if (okHttpClient.isPresent()) {
                            OkHttpClient.Builder builder = okHttpClient.get().newBuilder();
                            builder.authenticator((route, response) -> response.request().newBuilder().header("Authorization", indexerJwtAuthTokenProvider.get()).build());
                            Call call = builder.build().newCall(request);
                            try (Response response = call.execute()) {
                                if (response.isSuccessful()) {
                                    dataNodeProvisioningService.save(config.toBuilder().state(DataNodeProvisioningConfig.State.CONNECTED).build());
                                    LOG.info("Connectivity check successful with node {}", nodeId);
                                    return "true";
                                }
                                return "false";
                            }
                        } else {
                            return "false";
                        }
                    } catch (Exception e) {
                        // swallow exceptions during the first minute
                        if (counter.get() > (CONNECTION_ATTEMPTS / RATIO_WHEN_WE_START_SHOWING_EXCEPTIONS)) {
                            LOG.warn("Exception trying to connect to node " + config.nodeId() + ": " + e.getMessage() + ", retrying", e);
                        }
                        return "false";
                    }
                });
    }

    @NotNull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 2;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
