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
package org.graylog.datanode.opensearch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog2.datanode.DataNodeNotficationEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.TrustAllX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Certificate hot-reload writes the renewed certificate into {@link DatanodeKeystore} and hands it off to
 * OpenSearch by rewriting the HTTP/transport keystore files it watches - which isn't proof OpenSearch actually
 * reloaded them into its live TLS listener. This class closes that gap by opening a raw TLS handshake against the
 * process' own HTTP port and comparing the certificate it presents against what's expected.
 * <p>
 * Singleton: every {@link #verify(BigInteger, Supplier)} call supersedes whatever verification is still in flight,
 * so a reload triggered before a previous one's grace period elapses can't leave a stale check polling for a
 * certificate OpenSearch has already moved past - which would otherwise escalate a false "reload is stuck" warning.
 * <p>
 * TODO: only detects and reports drift, doesn't repair it - see {@link org.graylog.datanode.bootstrap.preflight.DataNodeCertRenewalPeriodical},
 *  which has the same blind spot from the other side. A follow-up could add a long-lived periodical that keeps
 *  comparing the master keystore against the actual served certificate (HTTP *and* transport) for the life of the
 *  process, re-triggering {@link OpensearchProcess#reloadCertificates()} on persistent drift and eventually
 *  forcing a restart.
 */
@Singleton
public class CertificateReloadVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(CertificateReloadVerifier.class);

    // OpenSearch's own file watcher for certificates_hot_reload polls periodically, so some delay is expected.
    static final Duration VERIFICATION_TIMEOUT = Duration.ofMinutes(2);

    static final Duration RETRY_INTERVAL = Duration.ofSeconds(5);

    static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(5);

    private final ClusterEventBus clusterEventBus;
    private final NodeId nodeId;
    private final Configuration configuration;
    private final Duration verificationTimeout;
    private final Duration retryInterval;
    private final Duration socketTimeout;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("certificate-reload-verifier-%d").build());

    // the verification currently in flight, if any; getAndSet()/compareAndSet() keep replace-current and
    // stop-iff-still-current atomic without an explicit lock.
    private final AtomicReference<PendingVerification> current = new AtomicReference<>();

    @Inject
    public CertificateReloadVerifier(ClusterEventBus clusterEventBus, NodeId nodeId, Configuration configuration) {
        this(clusterEventBus, nodeId, configuration, VERIFICATION_TIMEOUT, RETRY_INTERVAL, SOCKET_TIMEOUT);
    }

    @VisibleForTesting
    CertificateReloadVerifier(ClusterEventBus clusterEventBus, NodeId nodeId, Configuration configuration,
                               Duration verificationTimeout, Duration retryInterval, Duration socketTimeout) {
        this.clusterEventBus = clusterEventBus;
        this.nodeId = nodeId;
        this.configuration = configuration;
        this.verificationTimeout = verificationTimeout;
        this.retryInterval = retryInterval;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Queues verification that OpenSearch starts serving {@code expectedSerialNumber}, superseding whatever
     * verification is still in flight, and retries until confirmed or the grace period runs out.
     *
     * @param expectedSerialNumber serial to check for, or {@code null} if there's no signed certificate to verify
     *                             yet - cancels any in-flight verification but schedules nothing new.
     * @param httpBaseUrlSupplier looked up on every retry, since the process' base URL can change while a reload
     *                            is still pending.
     * @return a {@link Future} resolving once this verification finishes or is superseded; production callers can
     * ignore it.
     */
    public Future<Void> verify(@Nullable BigInteger expectedSerialNumber, Supplier<Optional<URI>> httpBaseUrlSupplier) {
        if (expectedSerialNumber == null) {
            supersede(null);
            return CompletableFuture.completedFuture(null);
        }

        LOG.info("Starting certificate hot-reload verification, expecting opensearch HTTP layer to start serving certificate with serial {}", expectedSerialNumber);
        final PendingVerification pendingVerification = PendingVerification.schedule(expectedSerialNumber, scheduler, retryInterval,
                pending -> checkOnce(pending, httpBaseUrlSupplier));
        supersede(pendingVerification);
        return pendingVerification.completed();
    }

    // makes replacement the current verification (or clears it, if null), stopping whatever was current before.
    private void supersede(@Nullable PendingVerification replacement) {
        final PendingVerification previous = current.getAndSet(replacement);
        if (previous != null) {
            previous.stop();
        }
    }

    private void checkOnce(PendingVerification pendingVerification, Supplier<Optional<URI>> httpBaseUrlSupplier) {
        // An uncaught exception here would silently kill this scheduleWithFixedDelay task without ever stopping it
        // - leaving it retrying forever with nothing useful left to check.
        try {
            final URI httpBaseUrl = httpBaseUrlSupplier.get().orElse(null);
            if (httpBaseUrl == null || !"https".equalsIgnoreCase(httpBaseUrl.getScheme())) {
                // nothing to verify (yet) without an HTTPS listener to probe - keep retrying, still within the
                // overall grace period checked below.
                failOrRetry(pendingVerification, null);
                return;
            }

            final BigInteger liveSerialNumber = fetchPeerCertificateSerial(httpBaseUrl.getHost(), httpBaseUrl.getPort());

            if (pendingVerification.expectedSerialNumber().equals(liveSerialNumber)) {
                LOG.info("Certificate hot reload verified, opensearch HTTP layer is now serving the expected certificate (serial {})",
                        liveSerialNumber);
                stopIfCurrent(pendingVerification);
                return;
            }

            failOrRetry(pendingVerification, liveSerialNumber);
        } catch (Exception e) {
            LOG.error("Unexpected error during certificate hot-reload verification, giving up", e);
            stopIfCurrent(pendingVerification);
        }
    }

    private void failOrRetry(PendingVerification pendingVerification, @Nullable BigInteger liveSerialNumber) {
        final Duration elapsed = Duration.between(pendingVerification.requestedAt(), Instant.now());
        if (elapsed.compareTo(verificationTimeout) < 0) {
            LOG.debug("Opensearch HTTP layer isn't serving the expected certificate yet (expected serial {}, currently serving {}), still within grace period ({} elapsed)",
                    pendingVerification.expectedSerialNumber(), liveSerialNumber, elapsed);
            return;
        }

        LOG.error("Certificate hot reload did not take effect on the opensearch HTTP layer within {}. Expected serial {}, but the process is still serving {}. " +
                        "This node most likely needs to be restarted to pick up the renewed certificate.",
                verificationTimeout, pendingVerification.expectedSerialNumber(), liveSerialNumber == null ? "<unreachable>" : liveSerialNumber);
        notifyReloadStuck();
        stopIfCurrent(pendingVerification);
    }

    // stops pendingVerification, but only if a newer verify() call hasn't already superseded it - compareAndSet
    // makes that check-and-clear atomic.
    private void stopIfCurrent(PendingVerification pendingVerification) {
        if (current.compareAndSet(pendingVerification, null)) {
            pendingVerification.stop();
        }
    }

    private void notifyReloadStuck() {
        clusterEventBus.post(new DataNodeNotficationEvent(nodeId.getNodeId(), Notification.Type.DATA_NODE_CERT_RENEWAL_WARNING,
                Notification.Severity.URGENT,
                Map.of("nodeName", configuration.getDatanodeNodeName(),
                        "reason", "Certificate hot reload did not take effect on the opensearch HTTP layer, node likely needs a restart")));
    }

    @Nullable
    private BigInteger fetchPeerCertificateSerial(String host, int port) {
        try {
            // Trust any certificate here: we only want to read what's being served, not validate it - rejecting an
            // untrusted/expired cert (the exact drift we're checking for) would fail the handshake before we could
            // inspect it. The result is only ever compared to the expected serial, never used for real traffic.
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new SecureRandom());
            try (SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket()) {
                socket.connect(new InetSocketAddress(host, port), (int) socketTimeout.toMillis());
                socket.setSoTimeout((int) socketTimeout.toMillis());
                socket.startHandshake();
                final Certificate[] peerCertificates = socket.getSession().getPeerCertificates();
                if (peerCertificates.length == 0 || !(peerCertificates[0] instanceof X509Certificate leaf)) {
                    return null;
                }
                return leaf.getSerialNumber();
            }
        } catch (Exception e) {
            LOG.debug("Failed to probe opensearch certificate at {}:{} for hot-reload verification", host, port, e);
            return null;
        }
    }
}
