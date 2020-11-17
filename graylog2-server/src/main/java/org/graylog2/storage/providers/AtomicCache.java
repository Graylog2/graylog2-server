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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AtomicCache<T> {
    private final AtomicReference<Future<T>> value;

    public AtomicCache() {
        this.value = new AtomicReference<>();
    }

    public T get(Supplier<T> valueSupplier) throws ExecutionException, InterruptedException {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        final Future<T> previous = this.value.getAndAccumulate(completableFuture, (prev, cur) -> prev == null ? cur : prev);
        if (previous == null) {
            final T newValue = valueSupplier.get();
            completableFuture.complete(newValue);

            return newValue;
        } else {
            return previous.get();
        }
    }
}
