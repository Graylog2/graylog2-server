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
package org.graylog2.database;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.mongodb.MongoException;
import org.graylog2.database.utils.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * MongoDB upsert requests can fail when they are creating a new entry concurrently.
 * https://jira.mongodb.org/browse/SERVER-14322
 * This helper can be used to retry upserts if they throw a duplicate key error
 *
 * @deprecated This class should not be used anymore. It was intended for usage with MongoDB versions < 4.2 which are
 * not supported for usage with Graylog anymore. Recent versions of MongoDB implement server-side retries for certain
 * upsert scenarios.
 */
@Deprecated(forRemoval = true)
public class MongoDBUpsertRetryer {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBUpsertRetryer.class);

    public static <T> T run(Callable<T> c) {
        final Retryer<T> retryer = RetryerBuilder.<T>newBuilder()
                .retryIfException(t -> t instanceof MongoException e && MongoUtils.isDuplicateKeyError(e))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasException()) {
                            LOG.debug("Upsert failed with {}. Retrying request", attempt.getExceptionCause().toString());
                        }
                    }
                })
                .build();
        try {
            return retryer.call(c);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (RetryException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
