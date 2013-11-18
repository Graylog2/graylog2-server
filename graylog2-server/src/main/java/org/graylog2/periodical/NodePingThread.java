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
package org.graylog2.periodical;

import org.graylog2.Core;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodePingThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingThread.class);

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 1;

    private final Core core;

    public NodePingThread(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        try {
            Node.thisNode(core).markAsAlive(core.isMaster(), core.getConfiguration().getRestTransportUri());
        } catch (NodeNotFoundException e) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            Node.registerServer(core, core.isMaster(), core.getConfiguration().getRestTransportUri());
        }

        // Remove old nodes that are no longer running. (Just some housekeeping)
        Node.dropOutdated(core);
    }

}
