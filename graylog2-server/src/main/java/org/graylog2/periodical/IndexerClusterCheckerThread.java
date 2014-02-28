package org.graylog2.periodical;

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.graylog2.Core;
import org.graylog2.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexerClusterCheckerThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerClusterCheckerThread.class);

    public static final int MINIMUM_OPEN_FILES_LIMIT = 64000;

    @Override
    public void run() {
        for (NodeInfo node : core.getIndexer().cluster().getDataNodes()) {
            // Check number of maximum open files.
            if (node.getProcess().getMaxFileDescriptors() < MINIMUM_OPEN_FILES_LIMIT) {
                LOG.info("Indexer node <{}> has a too low open file limit: [{}]",
                        node.getNode().getName(),
                        node.getProcess().getMaxFileDescriptors());

                // Write notification.
                Notification.buildNow(core)
                        .addType(Notification.Type.ES_OPEN_FILES)
                        .addSeverity(Notification.Severity.URGENT)
                        .publishIfFirst();
            }
        }
    }

    @Override
    public boolean runsForever() {
        return false;
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
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 30;
    }
}
