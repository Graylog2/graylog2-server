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
package org.graylog2.storage.providers;

import org.graylog2.plugin.Version;
import org.graylog2.storage.versionprobe.ElasticsearchProbeException;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class ElasticsearchVersionProvider implements Provider<Version> {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchVersionProvider.class);
    public static final String NO_HOST_REACHABLE_ERROR = "Unable to probe any host for Elasticsearch version";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Version> versionOverride;
    private final List<URI> elasticsearchHosts;
    private final VersionProbe versionProbe;
    private final AtomicCache<Optional<Version>> cachedVersion;
    private final int connectionRetries;
    private final Duration connectionRetryWait;

    @Inject
    public ElasticsearchVersionProvider(@Named("elasticsearch_version") @Nullable Version versionOverride,
                                        @Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                                        @Named("elasticsearch_connection_retries") int elasticsearchConnectionRetries,
                                        @Named("elasticsearch_connection_retry_wait") Duration elasticsearchConnectionRetryWait,
                                        VersionProbe versionProbe,
                                        AtomicCache<Optional<Version>> cachedVersion) {

        this.versionOverride = Optional.ofNullable(versionOverride);
        this.elasticsearchHosts = elasticsearchHosts;
        this.versionProbe = versionProbe;
        this.cachedVersion = cachedVersion;
        this.connectionRetries = elasticsearchConnectionRetries;
        this.connectionRetryWait = elasticsearchConnectionRetryWait;
    }

    private Optional<Version> probeForVersionAndRetry() throws ElasticsearchProbeException {
        // if we don't want to wait for ES to be up and there is an explicit version set, return
        if(connectionRetries < 1 && versionOverride.isPresent())
            return Optional.empty();

        int i = 0;
        // try at least once to satisfy if no explicit version is set but also if there should be no retries
        do {
            try {
                final Optional<Version> probedVersion = this.versionProbe.probe(this.elasticsearchHosts);
                if(probedVersion.isPresent()) {
                    LOG.info("Elasticsearch cluster is running v" + probedVersion.get());
                    return probedVersion;
                }
                if(i < this.connectionRetries) {
                    LOG.warn("Failed to connect to Elasticsearch. Retry {} from {}", i+1, this.connectionRetries);
                    Thread.sleep(this.connectionRetryWait.toMillis());
                }
            } catch (InterruptedException iex) {
                LOG.warn("Failed to connect to Elasticsearch. Retry {} from {}", i+1, this.connectionRetries);
            }
            i++;
        } while (i <= this.connectionRetries);

        throw new ElasticsearchProbeException(NO_HOST_REACHABLE_ERROR + "!");
    }

    private Version explicitVersion() {
        final Version explicitVersion = versionOverride.get();
        LOG.info("Elasticsearch version set to " + explicitVersion + " - disabling version probe.");
        return explicitVersion;
    }

    @Override
    public Version get() {
        try {
            final Version probedVersion = this.cachedVersion.get(this::probeForVersionAndRetry).orElseThrow(() -> new ElasticsearchProbeException(NO_HOST_REACHABLE_ERROR + "!"));
            return this.versionOverride.isPresent() ? explicitVersion() : probedVersion;
        } catch (ExecutionException | InterruptedException e) {
            throw new ElasticsearchProbeException(NO_HOST_REACHABLE_ERROR + ": ", e);
        }
    }
}
