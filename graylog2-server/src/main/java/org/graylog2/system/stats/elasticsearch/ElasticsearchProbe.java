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
package org.graylog2.system.stats.elasticsearch;

import com.google.inject.Singleton;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.PendingTasksStats;
import org.graylog2.indexer.indices.HealthStatus;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Singleton
public class ElasticsearchProbe {
    private final IndexSetRegistry indexSetRegistry;
    private final ClusterAdapter clusterAdapter;

    @Inject
    public ElasticsearchProbe(IndexSetRegistry indexSetRegistry, ClusterAdapter clusterAdapter) {
        this.indexSetRegistry = indexSetRegistry;
        this.clusterAdapter = clusterAdapter;
    }

    public ElasticsearchStats elasticsearchStats() {
        final ClusterStats clusterStats = clusterAdapter.clusterStats();

        final PendingTasksStats pendingTasksStats = clusterAdapter.pendingTasks();

        final List<String> indices = Arrays.asList(indexSetRegistry.getIndexWildcards());

        final ShardStats shardStats = clusterAdapter.shardStats(indices);
        final ClusterHealth clusterHealth = ClusterHealth.from(
                shardStats,
                pendingTasksStats
        );
        final HealthStatus healthStatus = clusterAdapter.health(indices).orElseThrow(() -> new IllegalStateException("Unable to retrieve cluster health."));

        return ElasticsearchStats.create(
                clusterStats.clusterName(),
                clusterStats.clusterVersion(),
                healthStatus,
                clusterHealth,
                clusterStats.nodesStats(),
                clusterStats.indicesStats()
        );
    }
}
