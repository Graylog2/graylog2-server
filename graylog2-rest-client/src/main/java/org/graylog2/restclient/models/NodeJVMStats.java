/**
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
