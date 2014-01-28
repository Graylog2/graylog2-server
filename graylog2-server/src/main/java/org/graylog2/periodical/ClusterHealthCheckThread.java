package org.graylog2.periodical;

import org.graylog2.Core;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterHealthCheckThread implements  Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHealthCheckThread.class);

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 20;

    private final Core core;

    public ClusterHealthCheckThread(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        try {
            if (core.inputs().runningCount() == 0) {
                LOG.debug("No input running in cluster!");
                getNotification().publishIfFirst();
            } else {
                LOG.debug("Running inputs found, disabling notification");
                getNotification().fixed();
            }
        } catch (NodeNotFoundException e) {
            LOG.error("Unable to find own node: ", e.getMessage(), e);
        }
    }

    protected Notification getNotification() throws NodeNotFoundException {
        Notification notification = Notification.buildNow(core);
        notification.addType(Notification.Type.NO_INPUT_RUNNING);
        notification.addSeverity(Notification.Severity.URGENT);
        notification.addNode(Node.thisNode(core));

        return notification;
    }
}
