package org.graylog2.periodical;

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterHealthCheckThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHealthCheckThread.class);

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
