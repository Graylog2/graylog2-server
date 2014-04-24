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

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodePingThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingThread.class);

    @Override
    public void run() {
        try {
            Node.thisNode(core).markAsAlive(core.isMaster(), core.getConfiguration().getRestTransportUri());
        } catch (NodeNotFoundException e) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            Node.registerServer(core, core.isMaster(), core.getConfiguration().getRestTransportUri());
        }
        try {
            // Remove old nodes that are no longer running. (Just some housekeeping)
            Node.dropOutdated(core);

            final ActivityWriter activityWriter = core.getActivityWriter();
            try {
                // Check that we still have a master node in the cluster, if not, warn the user.
                if (Node.thisNode(core).isAnyMasterPresent()) {
                    boolean removedNotification = Notification.build(core)
                            .addType(Notification.Type.NO_MASTER)
                            .fixed();
                    if (removedNotification) {
                        activityWriter.write(
                            new Activity("Notification condition [" + Notification.Type.NO_MASTER + "] " +
                                                 "has been fixed.", NodePingThread.class));
                    }
                } else {
                    Notification.buildNow(core)
                            .addThisNode()
                            .addType(Notification.Type.NO_MASTER)
                            .addSeverity(Notification.Severity.URGENT)
                            .publishIfFirst();
                }
            } catch (NodeNotFoundException e) {
                LOG.debug("Our node has immediately been purged again. This should not happen and indicates a clock skew.");
                /*Notification.buildNow(core)
                        .addThisNode()
                        .addType(Notification.Type.CHECK_SERVER_CLOCKS)
                        .addSeverity(Notification.Severity.URGENT)
                        .publishIfFirst();
                activityWriter.write(
                        new Activity("This graylog2 server node (" + core.getNodeId() + ") was immediately purged, " +
                                             "clock skew on other graylog2-server node is likely. Check your system clocks.",
                                     NodePingThread.class));
                */
                // Removed for now. https://github.com/Graylog2/graylog2-web-interface/issues/625
            }
        } catch (Exception e) {
            LOG.warn("Caught exception during node ping.", e);
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }
}
