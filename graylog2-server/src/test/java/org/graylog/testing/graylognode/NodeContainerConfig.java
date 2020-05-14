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
package org.graylog.testing.graylognode;

import org.apache.commons.lang.ArrayUtils;
import org.testcontainers.containers.Network;

import java.util.Arrays;

public class NodeContainerConfig {

    static final int API_PORT = 9000;
    static final int DEBUG_PORT = 5005;

    public final Network network;
    public final String mongoDbUri;
    public final String elasticsearchUri;
    public final int[] extraPorts;
    public final boolean enableDebugging;
    public final boolean skipPackaging;

    public static NodeContainerConfig create(Network network, String mongoDbUri, String elasticsearchUri, int[] extraPorts) {
        return new NodeContainerConfig(network, mongoDbUri, elasticsearchUri, extraPorts);
    }

    public NodeContainerConfig(Network network, String mongoDbUri, String elasticsearchUri, int[] extraPorts) {
        this.network = network;
        this.mongoDbUri = mongoDbUri;
        this.elasticsearchUri = elasticsearchUri;
        this.extraPorts = extraPorts == null ? new int[0] : extraPorts;
        this.enableDebugging = flagFromEnvVar("GRAYLOG_IT_DEBUG_SERVER");
        this.skipPackaging = flagFromEnvVar("GRAYLOG_IT_SKIP_PACKAGING");
    }

    private static boolean flagFromEnvVar(String flagName) {
        String flag = System.getenv(flagName);
        return flag != null && flag.equalsIgnoreCase("true");
    }

    public Integer[] portsToExpose() {
        int[] allPorts = ArrayUtils.add(extraPorts, 0, API_PORT);

        if (enableDebugging) {
            allPorts = ArrayUtils.add(allPorts, 0, DEBUG_PORT);
        }

        return Arrays.stream(allPorts).boxed().toArray(Integer[]::new);
    }
}
