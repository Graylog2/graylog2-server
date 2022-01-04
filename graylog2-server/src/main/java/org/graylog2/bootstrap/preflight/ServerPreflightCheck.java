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
import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoDBVersionCheck;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.ElasticsearchProbeException;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerPreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(ServerPreflightCheck.class);

    private final boolean skipPreflightChecks;
    private final int mongoVersionProbeAttempts;
    private final List<URI> elasticsearchHosts;
    private final VersionProbe elasticVersionProbe;
    private final MongoConnection mongoConnection;
    private MessageQueueWriter messageQueueWriter;

    @Inject
    public ServerPreflightCheck(@Named(value = "skip_preflight_checks") boolean skipPreflightChecks,
                                @Named(value = "mongodb_version_probe_attempts") int mongoVersionProbeAttempts,
                                @Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                                VersionProbe elasticVersionProbe, MongoConnection mongoConnection,
                                MessageQueueWriter messageQueueWriter) {
        this.skipPreflightChecks = skipPreflightChecks;
        this.mongoVersionProbeAttempts = mongoVersionProbeAttempts;
        this.elasticsearchHosts = elasticsearchHosts;
        this.elasticVersionProbe = elasticVersionProbe;
        this.mongoConnection = mongoConnection;
        this.messageQueueWriter = messageQueueWriter;
    }

    public void runChecks(Configuration configuration) {
        if (skipPreflightChecks) {
            LOG.info("Skipping preflight checks");
            return;
        }
        checkMongoDb();
        checkElasticsearch();
        checkJournal(configuration);
    }

    private void checkMongoDb() {
        try {
            final Version mongoVersion = RetryerBuilder.<Version>newBuilder()
                    .retryIfResult(Objects::isNull)
                    .retryIfExceptionOfType(MongoTimeoutException.class)
                    .retryIfRuntimeException()
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.getAttemptNumber() == 1) {
                                return;
                            }
                            if (mongoVersionProbeAttempts == 0) {
                                LOG.info("MongoDB is not available. Retry #{}", attempt.getAttemptNumber());
                            } else {
                                LOG.info("MongoDB is not available. Retry #{}/{}", attempt.getAttemptNumber(), mongoVersionProbeAttempts);
                            }
                        }
                    })
                    .withWaitStrategy(WaitStrategies.fixedWait(2, TimeUnit.SECONDS))
                    .withStopStrategy(mongoVersionProbeAttempts == 0 ? StopStrategies.neverStop() : StopStrategies.stopAfterAttempt(mongoVersionProbeAttempts))
                    .build()
                    .call(() -> {
                        try (MongoClient mongoClient = (MongoClient) mongoConnection.connect()) {
                            return MongoDBVersionCheck.getVersion(mongoClient);
                        }
                    });

            MongoDBVersionCheck.assertCompatibleVersion(mongoVersion);
            LOG.info("Connected to MongoDB version {}", mongoVersion);
        } catch (ExecutionException | RetryException e) {
            throw new PreflightCheckException("Failed to retrieve MongoDB version.", e);
        }
    }

    private void checkElasticsearch() {
        try {
            final Optional<SearchVersion> searchVersion = elasticVersionProbe.probe(elasticsearchHosts);
            searchVersion.orElseThrow(() -> new PreflightCheckException("Could not get Elasticsearch version"));

            // TODO add ES version constraints check here

            LOG.info("Connected to Elasticsearch version {}", searchVersion.get().toString());
        } catch (ElasticsearchProbeException e) {
            throw new PreflightCheckException(e);
        }
    }

    private void checkJournal(Configuration configuration) {
        if (!configuration.isMessageJournalEnabled()) {
            return;
        }
        if (configuration.getMessageJournalMode().equals(MessageQueueModule.DISK_JOURNAL_MODE)) {
            // TODO
        }
    }
}
