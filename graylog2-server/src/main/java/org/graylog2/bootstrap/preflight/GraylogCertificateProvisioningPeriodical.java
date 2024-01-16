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
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.graylog.security.certutil.CaConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.cert.storage.CertChainMongoStorage;
import org.graylog.security.certutil.cert.storage.CertChainStorage;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog2.Configuration;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

@Singleton
public class GraylogCertificateProvisioningPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertificateProvisioningPeriodical.class);
    private static final int THREADPOOL_THREADS = 5;
    private static final int CONNECTION_ATTEMPTS = 40;
    private static final int WAIT_BETWEEN_CONNECTION_ATTEMPTS = 3;
    private static final Duration DELAY_BEFORE_SHOWING_EXCEPTIONS = Duration.ofMinutes(1);
    private static final String ERROR_MESSAGE_PREFIX = "Error trying to connect to data node ";

    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NodeService<DataNodeDto> nodeService;

    private final CaConfiguration configuration;
    private final CsrMongoStorage csrStorage;
    private final CertChainStorage certMongoStorage;
    private final CaService caService;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;
    private final String passwordSecret;
    private final Supplier<OkHttpClient> okHttpClient;
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
                                                    final NodeService<DataNodeDto> nodeService,
                                                    final CsrSigner csrSigner,
                                                    final ClusterConfigService clusterConfigService,
                                                    final @Named("password_secret") String passwordSecret,
                                                    final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider,
                                                    final PreflightConfigService preflightConfigService,
                                                    final NotificationService notificationService,
                                                    final CustomCAX509TrustManager trustManager) {
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.csrStorage = csrStorage;
        this.certMongoStorage = certMongoStorage;
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.configuration = configuration;
        this.nodeService = nodeService;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
        this.preflightConfigService = preflightConfigService;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
        this.notificationService = notificationService;
        this.executor = Executors.newFixedThreadPool(THREADPOOL_THREADS, new ThreadFactoryBuilder().setNameFormat("provisioning-connectivity-check-task").build());
        this.okHttpClient = Suppliers.memoize(() -> buildConnectivityCheckOkHttpClient(trustManager));
    }

    // building a httpclient to check the connectivity to OpenSearch - TODO: maybe replace it with a VersionProbe already?
    private static OkHttpClient buildConnectivityCheckOkHttpClient(final X509TrustManager trustManager) {
        try {
            final var clientBuilder = new OkHttpClient.Builder();
            final var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            return clientBuilder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            LOG.error("Could not set Graylog CA trust manager: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
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
            if (!nodes.isEmpty()) {

                final var password = configuration.configuredCaExists()
                        ? configuration.getCaPassword().toCharArray()
                        : passwordSecret.toCharArray();
                final Optional<KeyStore> optKey = caService.loadKeyStore();
                if (optKey.isEmpty()) {
                    LOG.debug("No keystore available.");
                    return;
                }

                final var renewalPolicy = getRenewalPolicy();
                if (renewalPolicy == null) {
                    LOG.debug("No renewal policy available.");
                    return;
                }

                var nodesByState = nodes.stream().collect(Collectors.groupingBy(node -> Optional.ofNullable(node.state())
                        .orElse(DataNodeProvisioningConfig.State.UNCONFIGURED)));

                // if we're running in post-preflight and new datanodes arrive, they should configure themselves automatically
                var cfg = preflightConfigService.getPreflightConfigResult();
                if (cfg.equals(PreflightConfigResult.FINISHED)) {
                    var unconfiguredNodes = nodesByState.getOrDefault(DataNodeProvisioningConfig.State.UNCONFIGURED, List.of());
                    if (renewalPolicy.mode().equals(RenewalPolicy.Mode.AUTOMATIC)) {
                        unconfiguredNodes.forEach(c -> dataNodeProvisioningService.save(c.asConfigured()));
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
                final var nodesWithCSR = nodesByState.getOrDefault(DataNodeProvisioningConfig.State.CSR, List.of());
                final var hasNodesWithCSR = !nodesWithCSR.isEmpty();
                if (hasNodesWithCSR) {
                    var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, password);
                    var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);
                    nodesWithCSR.forEach(c -> {
                        try {
                            var csr = csrStorage.readCsr(c.nodeId());
                            if (csr.isEmpty()) {
                                LOG.error("Node in CSR state, but no CSR present : " + c.nodeId());
                                dataNodeProvisioningService.save(c.asError("Node in CSR state, but no CSR present"));
                            } else {
                                var cert = csrSigner.sign(caPrivateKey, caCertificate, csr.get(), renewalPolicy);
                                final List<X509Certificate> caCertificates = List.of(caCertificate);
                                certMongoStorage.writeCertChain(new CertificateChain(cert, caCertificates), c.nodeId());
                            }
                        } catch (Exception e) {
                            LOG.error("Could not sign CSR: " + e.getMessage(), e);
                            dataNodeProvisioningService.save(c.asError(e.getMessage()));
                        }
                    });
                }

                nodesByState.getOrDefault(DataNodeProvisioningConfig.State.STARTUP_REQUESTED, List.of())
                        .forEach(c -> {
                            dataNodeProvisioningService.save(c.asConnecting());
                            executor.submit(() -> checkConnectivity(c));
                        });
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException |
                 KeyStoreStorageException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkConnectivity(final DataNodeProvisioningConfig config) {
        LOG.info("Starting connectivity check with node {}, silencing error messages for {} seconds.", config.nodeId(), DELAY_BEFORE_SHOWING_EXCEPTIONS.getSeconds());
        final var nodeId = config.nodeId();
        final var retryer = RetryerBuilder.<Response>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(WAIT_BETWEEN_CONNECTION_ATTEMPTS, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(CONNECTION_ATTEMPTS))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.getDelaySinceFirstAttempt() > DELAY_BEFORE_SHOWING_EXCEPTIONS.toMillis()) {
                            if (attempt.hasException()) {
                                var e = attempt.getExceptionCause();
                                LOG.warn(ERROR_MESSAGE_PREFIX + " {}: {}, retrying (attempt #{})", config.nodeId(), e.getMessage(), attempt.getAttemptNumber());
                            } else {
                                LOG.warn(ERROR_MESSAGE_PREFIX + " {}, retrying (attempt #{})", config.nodeId(), attempt.getAttemptNumber());
                            }
                        }
                    }
                })
                .retryIfResult(response -> !response.isSuccessful())
                .retryIfException()
                .build();
        final Callable<Response> callable = () -> {
            final var node = nodeService.byNodeId(nodeId);
            final var request = new Request.Builder().url(node.getTransportAddress()).build();
            final var builder = okHttpClient.get().newBuilder()
                    .authenticator((route, response) -> response.request()
                            .newBuilder()
                            .header("Authorization", indexerJwtAuthTokenProvider.get())
                            .build());
            final var call = builder.build().newCall(request);
            return call.execute();
        };
        try (Response response = retryer.call(callable)) {
            var success = response.isSuccessful();
            if (success) {
                dataNodeProvisioningService.save(config.asConnected());
                LOG.info("Connectivity check successful with node {}", nodeId);
            } else {
                var errorMessage = response.message();
                dataNodeProvisioningService.save(config.asError("Data Node not reachable: " + errorMessage));
            }
        } catch (ExecutionException e) {
            LOG.error(ERROR_MESSAGE_PREFIX + " {}: {}", config.nodeId(), e.getMessage());
            dataNodeProvisioningService.save(config.asError(e.getMessage()));
        } catch (RetryException e) {
            LOG.error(ERROR_MESSAGE_PREFIX + " {}: {}", config.nodeId(), e.getMessage());
            var exceptionCause = Optional.ofNullable(e.getLastFailedAttempt().getExceptionCause()).orElse(e);
            var errorMsg = exceptionCause.getMessage();
            dataNodeProvisioningService.save(config.asError(errorMsg));
        }
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
