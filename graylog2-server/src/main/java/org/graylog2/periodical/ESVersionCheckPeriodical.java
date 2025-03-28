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
package org.graylog2.periodical;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbeFactory;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.graylog2.storage.versionprobe.VersionProbeLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class ESVersionCheckPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ESVersionCheckPeriodical.class);
    private final SearchVersion initialElasticsearchVersion;
    private final Optional<SearchVersion> versionOverride;
    private final VersionProbeFactory versionProbeFactory;
    private final NotificationService notificationService;
    private final List<URI> indexerHosts;
    private final boolean useJwtAuthentication;

    @Inject
    public ESVersionCheckPeriodical(@DetectedSearchVersion SearchVersion elasticsearchVersion,
                                    @Named("elasticsearch_version") @Nullable SearchVersion versionOverride,
                                    VersionProbeFactory versionProbeFactory,
                                    NotificationService notificationService,
                                    @IndexerHosts List<URI> indexerHosts,
                                    @Named("indexer_use_jwt_authentication") boolean useJwtAuthentication,
                                    @RunsWithDataNode Boolean runsWithDataNode
                                    ) {
        this.initialElasticsearchVersion = elasticsearchVersion;
        this.versionOverride = Optional.ofNullable(versionOverride);
        this.versionProbeFactory = versionProbeFactory;
        this.notificationService = notificationService;
        this.indexerHosts = indexerHosts;
        this.useJwtAuthentication = runsWithDataNode || useJwtAuthentication;
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
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 30;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        if (versionOverride.isPresent()) {
            LOG.debug("Elasticsearch version is set manually. Not running check.");
            return;
        }

        final VersionProbe limitedProbe = this.versionProbeFactory.create(1, Duration.seconds(1), useJwtAuthentication, VersionProbeLogger.INSTANCE);

        limitedProbe.probe(indexerHosts).ifPresent(version -> {
            if (compatible(this.initialElasticsearchVersion, version)) {
                notificationService.fixed(Notification.Type.ES_VERSION_MISMATCH);
            } else {
                LOG.warn("Elasticsearch version currently running ({}) is incompatible with the one Graylog was started " +
                        "with ({}) - a restart is required!", version, initialElasticsearchVersion);
                final Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.ES_VERSION_MISMATCH)
                        .addSeverity(Notification.Severity.URGENT)
                        .addDetail("initial_version", initialElasticsearchVersion.toString())
                        .addDetail("current_version", version.toString());
                notificationService.publishIfFirst(notification);
            }
        });
    }

    private boolean compatible(SearchVersion initialElasticsearchMajorVersion, SearchVersion version) {
        return initialElasticsearchMajorVersion.version().majorVersion() == version.version().majorVersion();
    }
}
