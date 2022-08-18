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
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.graylog2.database.MongoDBVersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoDBPreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBPreflightCheck.class);

    private final int mongoVersionProbeAttempts;
    private final MongoConnection mongoConnection;

    private final AtomicBoolean dbIsEmpty;

    @Inject
    public MongoDBPreflightCheck(@Named(value = "mongodb_version_probe_attempts") int mongoVersionProbeAttempts,
                                 MongoDbConfiguration configuration) {
        this.mongoVersionProbeAttempts = mongoVersionProbeAttempts;
        // We build our own MongoConnection instance here, so we can close it without interfering with other users
        this.mongoConnection = new MongoConnectionImpl(configuration);
        dbIsEmpty = new AtomicBoolean(false);
    }

    public boolean dbIsEmpty() {
        return dbIsEmpty.get();
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
                        try (MongoClient mongoClient = (MongoClient) mongoConnection.connect()) {
                            // Detect an empty, pristine database. It should have no collections
                            final String firstCollectionName = mongoConnection.getMongoDatabase().listCollectionNames().first();
                            dbIsEmpty.set(firstCollectionName == null);
                            return MongoDBVersionCheck.getVersion(mongoClient);
                        }
                    });

            MongoDBVersionCheck.assertCompatibleVersion(mongoVersion);
            LOG.info("Connected to MongoDB version {}", mongoVersion);
        } catch (ExecutionException | RetryException e) {
            throw new PreflightCheckException("Failed to retrieve MongoDB version.", e);
        }
    }
}
