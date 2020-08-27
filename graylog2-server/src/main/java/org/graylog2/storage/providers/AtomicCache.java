/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
