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
package org.graylog2.indexer.guava;

import com.google.common.cache.CacheLoader;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class FunctionCacheLoader<K, V> extends CacheLoader<K, V> {
    private final Function<K, V> loadingFunction;

    public FunctionCacheLoader(Function<K, V> loadingFunction) {
        this.loadingFunction = requireNonNull(loadingFunction, "loadingFunction must not be null");
    }

    @Override
    public V load(K key) throws Exception {
        return loadingFunction.apply(key);
    }
}