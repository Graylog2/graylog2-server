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

import org.graylog2.restclient.models.api.responses.ByteListing;
import org.graylog2.restclient.models.api.responses.system.ClusterEntityJVMStatsResponse;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodeJVMStats {

    private final String nodeId;
    private final String info;
    private final String pid;
    private final ByteListing maxMemory;
    private final ByteListing usedMemory;
    private final ByteListing totalMemory;
    private final ByteListing freeMemory;

    public NodeJVMStats(ClusterEntityJVMStatsResponse r) {
        this.nodeId = r.nodeId;
        this.info = r.info;
        this.pid = r.pid;

        this.maxMemory = r.maxMemory;
        this.usedMemory = r.usedMemory;
        this.totalMemory = r.totalMemory;
        this.freeMemory = r.freeMemory;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getInfo() {
        return info;
    }

    public String getPid() {
        return pid;
    }

    public ByteListing getMaxMemory() {
        return maxMemory;
    }

    public ByteListing getUsedMemory() {
        return usedMemory;
    }

    public ByteListing getTotalMemory() {
        return totalMemory;
    }

    public ByteListing getFreeMemory() {
        return freeMemory;
    }

    public int usedMemoryPercentage() {
        return Math.round((float) usedMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    public int totalMemoryPercentage() {
        return Math.round((float) totalMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    public static NodeJVMStats buildEmpty() {
        final ByteListing byteListing = new ByteListing();
        final ClusterEntityJVMStatsResponse clusterEntity = new ClusterEntityJVMStatsResponse();

        byteListing.bytes = 0;
        byteListing.kilobytes  = 0;
        byteListing.megabytes = 0;

        clusterEntity.nodeId = "unknown";
        clusterEntity.info = "unknown";
        clusterEntity.pid = "unknown";
        clusterEntity.maxMemory = byteListing;
        clusterEntity.usedMemory = byteListing;
        clusterEntity.totalMemory = byteListing;
        clusterEntity.freeMemory = byteListing;

        return new NodeJVMStats(clusterEntity);
    }
}
