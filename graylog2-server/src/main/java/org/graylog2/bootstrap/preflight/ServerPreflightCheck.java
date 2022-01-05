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

import com.github.joschi.jadconfig.util.Size;
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
import org.graylog2.shared.system.stats.fs.FsProbe;
import org.graylog2.shared.system.stats.fs.FsStats;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.ElasticsearchProbeException;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.graylog2.configuration.validators.ElasticsearchVersionValidator.SUPPORTED_ES_VERSIONS;

@SuppressWarnings("UnstableApiUsage")
public class ServerPreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(ServerPreflightCheck.class);

    private final boolean skipPreflightChecks;
    private final int mongoVersionProbeAttempts;
    private final List<URI> elasticsearchHosts;
    private final Path journalDirectory;
    private final Size journalMaxSize;
    private final VersionProbe elasticVersionProbe;
    private final MongoConnection mongoConnection;
    private final FsProbe fsProbe;

    @Inject
    public ServerPreflightCheck(@Named(value = "skip_preflight_checks") boolean skipPreflightChecks,
                                @Named(value = "mongodb_version_probe_attempts") int mongoVersionProbeAttempts,
                                @Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                                @Named("message_journal_dir") Path journalDirectory,
                                @Named("message_journal_max_size") Size journalMaxSize,
                                VersionProbe elasticVersionProbe,
                                MongoConnection mongoConnection,
                                FsProbe fsProbe
                                ) {
        this.skipPreflightChecks = skipPreflightChecks;
        this.mongoVersionProbeAttempts = mongoVersionProbeAttempts;
        this.elasticsearchHosts = elasticsearchHosts;
        this.journalDirectory = journalDirectory;
        this.journalMaxSize = journalMaxSize;
        this.elasticVersionProbe = elasticVersionProbe;
        this.mongoConnection = mongoConnection;
        this.fsProbe = fsProbe;
    }

    public void runChecks(Configuration configuration) {
        if (skipPreflightChecks) {
            LOG.info("Skipping preflight checks");
            return;
        }
        checkMongoDb();
        checkElasticsearch();
        checkDiskJournal(configuration);
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
            final SearchVersion searchVersion = elasticVersionProbe.probe(elasticsearchHosts)
                    .orElseThrow(() -> new PreflightCheckException("Could not get Elasticsearch version"));

            if (SUPPORTED_ES_VERSIONS.stream().noneMatch(searchVersion::satisfies)) {
                throw new PreflightCheckException(StringUtils.f("Unsupported (Elastic/Open)Search version <%s>. Supported versions: <%s>",
                        searchVersion, SUPPORTED_ES_VERSIONS));
            }

            LOG.info("Connected to (Elastic/Open)Search version <{}>", searchVersion);
        } catch (ElasticsearchProbeException e) {
            throw new PreflightCheckException(e);
        }
    }

    private void checkDiskJournal(Configuration configuration) {
        if (!configuration.isMessageJournalEnabled() || (!configuration.getMessageJournalMode().equals(MessageQueueModule.DISK_JOURNAL_MODE))) {
            return;
        }
        if (!java.nio.file.Files.exists(journalDirectory)) {
            try {
                java.nio.file.Files.createDirectories(journalDirectory);
            } catch (IOException e) {
                throw new PreflightCheckException(StringUtils.f("Cannot create journal directory at <%s>", journalDirectory.toAbsolutePath()));
            }
        }
        if (!Files.isWritable(journalDirectory)) {
            throw new PreflightCheckException(StringUtils.f("Journal directory <%s> is not writable!", journalDirectory.toAbsolutePath()));
        }

        final Map<String, FsStats.Filesystem> filesystems = fsProbe.fsStats().filesystems();
        final FsStats.Filesystem journalFs = filesystems.get(journalDirectory.toAbsolutePath().toString());
        if (journalFs != null) {
            if (journalFs.available() < journalMaxSize.toBytes()) {
                throw new PreflightCheckException(StringUtils.f(
                        "Journal directory <%s> has not enough free space (%d MB) to contain 'message_journal_max_size = %d MB' ",
                        journalDirectory.toAbsolutePath(),
                        Size.bytes(journalFs.available()).toMegabytes(),
                        journalMaxSize.toMegabytes()
                ));
            }
        } else {
            LOG.warn("Could not perform size check on journal directory <{}>", journalDirectory.toAbsolutePath());
        }
    }
}
