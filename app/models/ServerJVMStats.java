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
import models.api.responses.ByteListing;
import models.api.responses.system.ServerJVMStatsResponse;

import java.io.IOException;
import java.net.URL;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ServerJVMStats {

    public final String info;
    public final String pid;
    public final ByteListing maxMemory;
    public final ByteListing usedMemory;
    public final ByteListing totalMemory;
    public final ByteListing freeMemory;

    public ServerJVMStats(ServerJVMStatsResponse r) {
        this.info = r.info;
        this.pid = r.pid;

        this.maxMemory = r.maxMemory;
        this.usedMemory = r.usedMemory;
        this.totalMemory = r.totalMemory;
        this.freeMemory = r.freeMemory;
    }

    public int usedMemoryPercentage() {
        return Math.round((float) usedMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    public int totalMemoryPercentage() {
        return Math.round((float) totalMemory.getMegabytes() / maxMemory.getMegabytes() * 100);
    }

    // TODO make multi-node compatible
    public static ServerJVMStats get() throws IOException, APIException {
        URL url = Api.buildTarget("system/jvm");
        ServerJVMStatsResponse r = Api.get(url, ServerJVMStatsResponse.class);

        return new ServerJVMStats(r);
    }

}
