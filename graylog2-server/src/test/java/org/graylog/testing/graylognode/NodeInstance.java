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

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.graylog.testing.graylognode.NodeContainerFactory.API_PORT;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    private final GenericContainer<?> container;

    public static NodeInstance createStarted(Network network, String mongoDbUri, String elasticsearchUri, int[] extraPorts) {
        NodeContainerConfig config = NodeContainerConfig.create(network, mongoDbUri, elasticsearchUri, extraPorts);
        GenericContainer<?> container = NodeContainerFactory.buildContainer(config);
        return new NodeInstance(container);
    }

    public NodeInstance(GenericContainer<?> container) {
        this.container = container;
    }

    public void restart() {
        Stopwatch sw = Stopwatch.createStarted();
        container.stop();
        container.start();
        sw.stop();
        LOG.info("Restarted node container in " + sw.elapsed(TimeUnit.SECONDS));
    }

    public String uri() {
        return String.format(Locale.US, "http://%s", container.getContainerIpAddress());
    }

    public int apiPort() {
        return mappedPortFor(API_PORT);
    }

    public int mappedPortFor(int originalPort) {
        return container.getMappedPort(originalPort);
    }

    public void printLog() {
        System.out.println(container.getLogs());
    }
}
