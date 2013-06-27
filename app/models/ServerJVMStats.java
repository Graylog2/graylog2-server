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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lib.APIException;
import lib.Api;
import models.api.responses.ByteListing;
import models.api.responses.system.ServerJVMStatsResponse;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ServerJVMStats {

    private final String nodeId;
    private final String info;
    private final String pid;
    private final boolean isProcessing;
    private final ByteListing maxMemory;
    private final ByteListing usedMemory;
    private final ByteListing totalMemory;
    private final ByteListing freeMemory;

    public ServerJVMStats(ServerJVMStatsResponse r) {
        this.nodeId = r.nodeId;
        this.info = r.info;
        this.pid = r.pid;

        this.maxMemory = r.maxMemory;
        this.usedMemory = r.usedMemory;
        this.totalMemory = r.totalMemory;
        this.freeMemory = r.freeMemory;
        this.isProcessing = r.isProcessing;
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

    public boolean isProcessing() {
        return isProcessing;
    }

    public int usedMemoryPercentage() {
        return Math.round((float) usedMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    public int totalMemoryPercentage() {
        return Math.round((float) totalMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    public static List<ServerJVMStats> get() throws IOException, APIException {
        List<ServerJVMStats> result = Lists.newArrayList();
        List<ServerJVMStatsResponse> rs = Api.getFromAllNodes("system/jvm", ServerJVMStatsResponse.class);

        for (ServerJVMStatsResponse r : rs) {
            result.add(new ServerJVMStats(r));
        }

        return result;
    }

}
