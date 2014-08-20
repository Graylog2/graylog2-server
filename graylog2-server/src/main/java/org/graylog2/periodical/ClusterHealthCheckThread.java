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
package org.graylog2.periodical;

import com.google.inject.Inject;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterHealthCheckThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHealthCheckThread.class);
    private NotificationService notificationService;
    private final InputRegistry inputRegistry;
    private final NodeId nodeId;

    @Inject
    public ClusterHealthCheckThread(NotificationService notificationService,
                                    InputRegistry inputRegistry,
                                    NodeId nodeId) {
        this.notificationService = notificationService;
        this.inputRegistry = inputRegistry;
        this.nodeId = nodeId;
    }

    @Override
    public void doRun() {
        try {
            if (inputRegistry.runningCount() == 0) {
                LOG.debug("No input running in cluster!");
                notificationService.publishIfFirst(getNotification());
            } else {
                LOG.debug("Running inputs found, disabling notification");
                notificationService.fixed(getNotification());
            }
        } catch (NodeNotFoundException e) {
            LOG.error("Unable to find own node: ", e.getMessage(), e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected Notification getNotification() throws NodeNotFoundException {
        Notification notification = notificationService.buildNow();
        notification.addType(Notification.Type.NO_INPUT_RUNNING);
        notification.addSeverity(Notification.Severity.URGENT);
        notification.addNode(nodeId.toString());

        return notification;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 20;
    }

}
