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
package org.graylog.storage.elasticsearch7;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.ActionListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class FutureActionListener<T> implements ActionListener<T> {

    private final CompletableFuture<T> future;

    public FutureActionListener() {
        this.future = new CompletableFuture<T>();
    }

    @Override
    public void onResponse(T result) {
        future.complete(result);
    }

    @Override
    public void onFailure(Exception e) {
        future.completeExceptionally(e);
    }

    public T getBlocking(Duration timeout) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(timeout.getQuantity(), timeout.getUnit());
    }
}
