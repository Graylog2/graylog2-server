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
import com.mongodb.client.model.Filters;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.graylog2.database.MongoDBVersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoDBPreflightCheck implements PreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBPreflightCheck.class);
    private static final String CLUSTER_CONFIG_COLLECTION_NAME = "cluster_config";

    private final int mongoVersionProbeAttempts;
    private final MongoConnection mongoConnection;

    private final AtomicBoolean isFreshInstallation;

    @Inject
    public MongoDBPreflightCheck(@Named(value = "mongodb_version_probe_attempts") int mongoVersionProbeAttempts,
                                 MongoDbConfiguration configuration) {
        this.mongoVersionProbeAttempts = mongoVersionProbeAttempts;
        // We build our own MongoConnection instance here, so we can close it without interfering with other users
        this.mongoConnection = new MongoConnectionImpl(configuration);
        isFreshInstallation = new AtomicBoolean(false);
    }

    public boolean isFreshInstallation() {
        return isFreshInstallation.get();
    }

    public void runCheck() throws PreflightCheckException {
        try {
            final Version mongoVersion = RetryerBuilder.<Version>newBuilder()
                    .retryIfResult(Objects::isNull)
                    .retryIfExceptionOfType(MongoTimeoutException.class)
                    .retryIfRuntimeException()
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.hasResult()) {
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
                        try (MongoClient mongoClient = mongoConnection.connect()) {
                            detectFreshInstallation();
                            return MongoDBVersionCheck.getVersion(mongoClient);
                        }
                    });

            MongoDBVersionCheck.assertCompatibleVersion(mongoVersion);
            LOG.info("Connected to MongoDB version {}", mongoVersion);
        } catch (ExecutionException | RetryException e) {
            throw new PreflightCheckException("Failed to retrieve MongoDB version.", e);
        }
    }

    /**
     * The fresh installation detection is based on the presence of a document in the `cluster_config` collection which
     * is of type `org.graylog2.plugin.cluster.ClusterId`. This document is created by initial migrations. If this
     * document exists, we assume that the installation is not fresh. We can't use empty database for this
     * verification - datanodes may register itself into the nodes collection and maybe even persist some more
     * information in other collections.
     */
    private void detectFreshInstallation() {
        final boolean collectionExists = mongoConnection.getMongoDatabase()
                .listCollectionNames()
                .into(new HashSet<>())
                .contains(CLUSTER_CONFIG_COLLECTION_NAME);
        final boolean configurationExists = collectionExists && mongoConnection.getMongoDatabase()
                .getCollection("cluster_config")
                .find(Filters.eq("type", "org.graylog2.plugin.cluster.ClusterId"))
                .first() != null;
        this.isFreshInstallation.set(!configurationExists);
    }
}
