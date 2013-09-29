package org.graylog2.periodical;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.graylog2.Core;
import org.graylog2.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexerClusterCheckerThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerClusterCheckerThread.class);

    public static final int MINIMUM_OPEN_FILES_LIMIT = 64000;

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 30;

    private final Core core;

    public IndexerClusterCheckerThread(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        for (NodeInfo node : core.getIndexer().cluster().getDataNodes()) {
            // Check number of maximum open files.
            if (node.getProcess().getMaxFileDescriptors() < MINIMUM_OPEN_FILES_LIMIT) {
                LOG.info("Indexer node <{}> has a too low open file limit: [{}]",
                        node.getNode().getName(),
                        node.getProcess().getMaxFileDescriptors());

                // Write notification.
                if (Notification.isFirst(core, Notification.Type.ES_OPEN_FILES)) {
                    Notification.publish(core, Notification.Type.ES_OPEN_FILES, Notification.Severity.URGENT);
                }
            }
        }

    }


}
