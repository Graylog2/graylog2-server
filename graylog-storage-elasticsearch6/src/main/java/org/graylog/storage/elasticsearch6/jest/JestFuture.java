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
package org.graylog.storage.elasticsearch6.jest;

import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.client.JestResultHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class JestFuture<T> implements JestResultHandler<T> {

    private final CompletableFuture<T> future;

    public JestFuture() {
        future = new CompletableFuture<>();
    }

    @Override
    public void completed(T result) {
        future.complete(result);
    }

    @Override
    public void failed(Exception ex) {
        future.completeExceptionally(ex);
    }

    public T getBlocking(Duration timeout) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(timeout.getQuantity(), timeout.getUnit());
    }
}
