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
package org.graylog.testing.completebackend;

import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;

public abstract class SearchServerBuilder<T extends SearchServerInstance> {
    public static final String DEFAULT_HEAP_SIZE = "2g";
    private final SearchVersion version;
    private String hostname = "indexer";
    private Network network;
    private String heapSize = DEFAULT_HEAP_SIZE;
    private List<String> featureFlags = List.of();
    private String mongoDbUri;
    private String passwordSecret;
    private String rootPasswordSha2;

    public SearchServerBuilder(final SearchVersion version) {
        this.version = version;
    }

    public SearchVersion getVersion() {
        return version;
    }

    public SearchServerBuilder<T> network(final Network network) {
        this.network = network;
        return this;
    }

    public Network getNetwork() {
        return network;
    }

    public SearchServerBuilder<T> heapSize(final String heapSize) {
        this.heapSize = heapSize;
        return this;
    }

    public String getHeapSize() {
        return heapSize;
    }

    public SearchServerBuilder<T> featureFlags(final List<String> featureFlags) {
        this.featureFlags = new ArrayList<>(featureFlags);
        return this;
    }

    public List<String> getFeatureFlags() {
        return featureFlags;
    }

    public SearchServerBuilder<T> mongoDbUri(final String mongoDbUri) {
        this.mongoDbUri = mongoDbUri;
        return this;
    }

    public String getMongoDbUri() {
        return mongoDbUri;
    }

    public SearchServerBuilder<T> passwordSecret(final String passwordSecret) {
        this.passwordSecret = passwordSecret;
        return this;
    }

    public String getPasswordSecret() {
        return passwordSecret;
    }

    public SearchServerBuilder<T> rootPasswordSha2(final String rootPasswordSha2) {
        this.rootPasswordSha2 = rootPasswordSha2;
        return this;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }

    public SearchServerBuilder<T> hostname(final String hostname) {
        this.hostname = hostname;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    protected abstract T instantiate();

    public T build() {
        return instantiate();
    }
}
