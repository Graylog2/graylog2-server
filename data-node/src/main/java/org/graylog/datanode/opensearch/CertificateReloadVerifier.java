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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Certificate hot-reload on the datanode is a two-step process: {@link DatanodeKeystore} gets the renewed
 * certificate written into it, and that is then handed off to OpenSearch by rewriting the HTTP/transport keystore
 * files it watches (see {@code plugins.security.ssl.certificates_hot_reload.enabled}). Writing those files is not
 * proof that OpenSearch actually reloaded them into its live TLS listener - this class closes that gap by opening
 * a raw TLS handshake against the process' own HTTP port and comparing the certificate it actually presents against
 * what we expect it to be serving.
 * <p>
 * Each instance is a single, throwaway verification attempt: construct one and call {@link #start(Supplier)} right
 * after triggering a certificate reload, and it retries on its own short internal schedule for a bounded grace
 * period before stopping itself, either because the reload was confirmed or because it escalated. There's no
 * shared/long-lived state and no always-on periodical involved - construct a new instance for every reload rather
 * than reusing one.
 * <p>
 * TODO: this only detects and reports drift, it doesn't repair it - once {@link #VERIFICATION_TIMEOUT} is hit we
 *  just notify and stop, and nothing keeps checking afterwards. {@link org.graylog.datanode.bootstrap.preflight.DataNodeCertRenewalPeriodical}
 *  has the same blind spot from the other side: once the master keystore has a fresh cert it considers the job
 *  done, regardless of whether that cert ever actually reached OpenSearch's live listener. A follow-up could add a
 *  long-lived, low-frequency periodical that keeps comparing the master keystore against the actual served
 *  certificate (HTTP *and* transport, unlike this class) for the life of the process, re-triggers
 *  {@link OpensearchProcess#reloadCertificates()} on persistent drift, and - if that still doesn't converge after
 *  a much longer window - forces a restart via the same trigger {@link org.graylog.datanode.opensearch.statemachine.OpensearchState#FAILED}
 *  recovery would need anyway (see the TODO on {@code OpensearchStateMachine}'s FAILED state).
 */
public class CertificateReloadVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(CertificateReloadVerifier.class);

    /**
     * How long we keep re-checking after a reload before giving up and escalating. OpenSearch's own file watcher
     * for {@code certificates_hot_reload} polls periodically, so some delay after writing the files is expected.
     */
    static final Duration VERIFICATION_TIMEOUT = Duration.ofMinutes(2);

    static final Duration RETRY_INTERVAL = Duration.ofSeconds(5);

    static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(5);

    private final DatanodeKeystore datanodeKeystore;
    private final ClusterEventBus clusterEventBus;
    private final NodeId nodeId;
    private final Duration verificationTimeout;
    private final Duration retryInterval;
    private final Duration socketTimeout;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("certificate-reload-verifier-%d").build());

    public CertificateReloadVerifier(DatanodeKeystore datanodeKeystore, ClusterEventBus clusterEventBus, NodeId nodeId) {
        this(datanodeKeystore, clusterEventBus, nodeId, VERIFICATION_TIMEOUT, RETRY_INTERVAL, SOCKET_TIMEOUT);
    }

    @VisibleForTesting
    CertificateReloadVerifier(DatanodeKeystore datanodeKeystore, ClusterEventBus clusterEventBus, NodeId nodeId,
                              Duration verificationTimeout, Duration retryInterval, Duration socketTimeout) {
        this.datanodeKeystore = datanodeKeystore;
        this.clusterEventBus = clusterEventBus;
        this.nodeId = nodeId;
        this.verificationTimeout = verificationTimeout;
        this.retryInterval = retryInterval;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Snapshots whatever certificate is currently in the datanode keystore as the one we now expect OpenSearch's
     * HTTP layer to start serving, and starts checking for that on this instance's own short-lived schedule until
     * it's confirmed or the grace period runs out. The underlying executor shuts itself down once that happens, so
     * this instance is done and can be discarded after that - it's not meant to be started more than once.
     *
     * @param httpBaseUrlSupplier looked up on every retry (rather than once upfront) since the process' base URL
     *                            can legitimately be absent/change while a reload is still pending, e.g. right
     *                            after a reload was triggered while OpenSearch is still starting up.
     */
    public void start(Supplier<Optional<URI>> httpBaseUrlSupplier) {
        final BigInteger expectedSerialNumber = datanodeKeystore.getCertificateSerialNumber();
        if (expectedSerialNumber == null) {
            scheduler.shutdown();
            return;
        }

        LOG.debug("Expecting opensearch HTTP layer to start serving certificate with serial {} after hot reload", expectedSerialNumber);
        final PendingVerification pendingVerification = new PendingVerification(expectedSerialNumber, Instant.now());

        scheduler.scheduleWithFixedDelay(() -> checkOnce(pendingVerification, httpBaseUrlSupplier),
                retryInterval.toMillis(), retryInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * @return {@code true} once this instance has actually finished (matched or escalated), {@code false} if
     * {@code timeout} elapsed while it was still retrying. Any notification this run was ever going to send has
     * already been posted (synchronously) by the time this returns {@code true} - see {@link #failOrRetry}.
     */
    @VisibleForTesting
    boolean awaitCompletion(Duration timeout) throws InterruptedException {
        return scheduler.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void checkOnce(PendingVerification pendingVerification, Supplier<Optional<URI>> httpBaseUrlSupplier) {
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
            // this stops future runs of the scheduleWithFixedDelay task; it's safe to call from within the task
            // itself since it only prevents the *next* run from being scheduled, it doesn't interrupt this one.
            scheduler.shutdown();
            return;
        }

        failOrRetry(pendingVerification, liveSerialNumber);
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
        scheduler.shutdown();
    }

    private void notifyReloadStuck() {
        clusterEventBus.post(new DataNodeNotficationEvent(nodeId.getNodeId(), Notification.Type.CERTIFICATE_NEEDS_RENEWAL,
                Map.of("reason", "Certificate hot reload did not take effect on the opensearch HTTP layer, node likely needs a restart")));
    }

    @Nullable
    private BigInteger fetchPeerCertificateSerial(String host, int port) {
        try {
            // We deliberately trust any certificate here: this handshake exists only to read whatever leaf
            // certificate the process is currently presenting, not to validate it. Rejecting an untrusted/expired
            // cert (the exact failure mode we're trying to detect) would make the handshake fail before we ever
            // get to inspect it - which is the opposite of what this check needs. The result is only ever compared
            // against the expected serial number below; it's never used for real traffic or treated as trusted.
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

    private record PendingVerification(BigInteger expectedSerialNumber, Instant requestedAt) {
    }
}
