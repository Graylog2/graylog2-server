/**
 * Copyright 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import org.graylog2.buffers.BufferWatermark;
import org.graylog2.plugin.Tools;

/**
 * Filling the server_values collection
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValue {

    private final Core graylogServer;

    public ServerValue(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    public void setStartupTime(int timestamp) {
        set("startup_time", timestamp);
    }

    public void setPID(int pid) {
        set("pid", pid);
    }

    public void setJREInfo(String info) {
        set("jre", info);
    }

    public void setAvailableProcessors(int processors) {
        set("available_processors", processors);
    }

    public void setGraylog2Version(String version) {
        set("graylog2_version", version);
    }

    public void setLocalHostname(String hostname) {
        set("local_hostname", hostname);
    }

    public void writeThroughput(int current, int highest) {
        graylogServer.getMongoBridge().writeThroughput(graylogServer.getServerId(), current, highest);
    }
    
    public void writeBufferWatermarks(BufferWatermark outputBuffer, BufferWatermark processBuffer) {
        graylogServer.getMongoBridge().writeBufferWatermarks(graylogServer.getServerId(), outputBuffer, processBuffer);
    }
    
    public void writeMasterCacheSizes(int inputCacheSize, int outputCacheSize) {
        graylogServer.getMongoBridge().writeMasterCacheSizes(graylogServer.getServerId(), inputCacheSize, outputCacheSize);
    }
    
    public void setIsMaster(boolean isIt) {
        set("is_master", isIt);
    }

    public void ping() {
        set("ping", Tools.getUTCTimestamp());
    }

    private void set(String key, Object value) {
        graylogServer.getMongoBridge().setSimpleServerValue(graylogServer.getServerId(), key, value);
    }

}