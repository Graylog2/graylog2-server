/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.ClusterHealthStatus;
import org.graylog2.restclient.models.api.responses.system.ESClusterHealthResponse;

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
