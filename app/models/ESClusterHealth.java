/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import lib.APIException;
import lib.Api;
import lib.ClusterHealthStatus;
import models.api.responses.system.ESClusterHealthResponse;

import java.io.IOException;
import java.net.URL;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ESClusterHealth {

    private final ClusterHealthStatus status;
    private final int relocatingShards;
    private final int unassignedShards;
    private final int activeShards;
    private final int initializingShards;

    public ESClusterHealth(ESClusterHealthResponse r) {
        this.status = ClusterHealthStatus.valueOf(r.status.toUpperCase());
        this.activeShards = r.shards.active;
        this.initializingShards = r.shards.initializing;
        this.relocatingShards = r.shards.relocating;
        this.unassignedShards = r.shards.unassigned;
    }

    public static ESClusterHealth get() throws IOException, APIException {
        ESClusterHealthResponse r = Api.get("system/indexer/cluster/health", ESClusterHealthResponse.class);

        return new ESClusterHealth(r);
    }

    public ClusterHealthStatus getStatus() {
        return status;
    }

    public boolean isGreen() {
        return this.status == ClusterHealthStatus.GREEN;
    }

    public boolean isYellow() {
        return this.status == ClusterHealthStatus.YELLOW;
    }

    public boolean isRed() {
        return this.status == ClusterHealthStatus.RED;
    }

    public int getRelocatingShards() {
        return relocatingShards;
    }

    public int getUnassignedShards() {
        return unassignedShards;
    }

    public int getActiveShards() {
        return activeShards;
    }

    public int getInitializingShards() {
        return initializingShards;
    }
}
