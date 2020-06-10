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
package org.graylog2.indexer.cluster;

import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ClusterAdapter {
    Optional<HealthStatus> health(Collection<String> indices);

    Optional<HealthStatus> deflectorHealth(Collection<String> indices);

    Set<NodeFileDescriptorStats> fileDescriptorStats();

    Set<NodeDiskUsageStats> diskUsageStats();

    ClusterAllocationDiskSettings clusterAllocationDiskSettings();

    Optional<String> nodeIdToName(String nodeId);

    Optional<String> nodeIdToHostName(String nodeId);

    boolean isConnected();

    Optional<String> clusterName(Collection<String> indices);

    Optional<ClusterHealth> clusterHealthStats(Collection<String> indices);

    ElasticsearchStats statsForIndices(Collection<String> indices);
}
