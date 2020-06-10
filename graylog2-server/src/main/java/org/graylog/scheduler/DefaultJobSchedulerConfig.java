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
package org.graylog.scheduler;

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This is the default {@link JobSchedulerConfig}.
 */
public class DefaultJobSchedulerConfig implements JobSchedulerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobSchedulerConfig.class);

    private final NodeService nodeService;
    private final NodeId nodeId;

    @Inject
    public DefaultJobSchedulerConfig(NodeService nodeService, NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
    }

    @Override
    public boolean canStart() {
        try {
            return nodeService.byNodeId(nodeId).isPrimary();
        } catch (NodeNotFoundException e) {
            LOG.error("Couldn't find current node <{}> in the database", nodeId.toString(), e);
            return false;
        }
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public int numberOfWorkerThreads() {
        return 5;
    }
}
