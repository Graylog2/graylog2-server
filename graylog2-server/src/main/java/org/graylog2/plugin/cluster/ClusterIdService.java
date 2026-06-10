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
package org.graylog2.plugin.cluster;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Provides access to the {@link ClusterId} value.
 */
@Singleton
public class ClusterIdService {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public ClusterIdService(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Returns the cluster ID string value.
     *
     * @return the cluster ID string
     * @throws IllegalStateException when the cluster ID doesn't exist in the database or the string value is blank
     */
    @NonNull
    public String getString() {
        return get().clusterId();
    }

    /**
     * Returns the cluster ID object.
     *
     * @return the cluster ID object
     * @throws IllegalStateException when the cluster ID doesn't exist in the database or the string value is blank
     */
    @NonNull
    public ClusterId get() {
        final var clusterId = clusterConfigService.get(ClusterId.class);
        if (clusterId == null) {
            throw new IllegalStateException("Cluster ID doesn't exist");
        }
        if (isBlank(clusterId.clusterId())) {
            throw new IllegalStateException("Cluster ID is blank");
        }
        return clusterId;
    }
}
