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
package org.graylog2.indexer.esplugin;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.google.common.collect.Maps;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.graylog2.indexer.cluster.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;

public class ClusterStateMonitor extends AbstractLifecycleComponent<ClusterStateMonitor> implements ClusterStateListener {
    private static final Logger log = LoggerFactory.getLogger(ClusterStateMonitor.class);
    private static final EnumSet<ClusterState.ClusterStateStatus> VALID_CLUSTER_STATES =
            EnumSet.of(ClusterState.ClusterStateStatus.BEING_APPLIED, ClusterState.ClusterStateStatus.APPLIED);

    private final ClusterService clusterService;

    // Yes, this sucks, but ES and Graylog use different injectors and it's not obvious how to bridge them, so I'm using a static. Shoot me.
    private static Cluster cluster;

    @org.elasticsearch.common.inject.Inject
    public ClusterStateMonitor(Settings settings, ClusterService clusterService) {
        super(settings);
        this.clusterService = clusterService;
    }

    public static void setCluster(Cluster cluster) {
        ClusterStateMonitor.cluster = cluster;
    }


    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (!VALID_CLUSTER_STATES.contains(event.state().status())) {
            // Only process fully applied cluster states
            return;
        }

        if (event.state().getNodes().masterAndDataNodes().isEmpty()) {
            log.warn("No Elasticsearch data nodes in cluster, cluster is completely offline.");
        }
        if (!event.nodesChanged()) {
            // ignore events that don't contain node changes, we don't need to track this now
            return;
        }
        if (cluster != null) {
            final Map<String, DiscoveryNode> nodes = Maps.newHashMap();
            for (ObjectObjectCursor<String, DiscoveryNode> cursor : event.state().getNodes().dataNodes()) {
                nodes.put(cursor.key, cursor.value);
            }
            cluster.updateDataNodeList(nodes);
        }
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        clusterService.add(this);
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        clusterService.remove(this);
    }

    @Override
    protected void doClose() throws ElasticsearchException {

    }
}
