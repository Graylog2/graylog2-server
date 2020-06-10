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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.indices.HealthStatus;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ElasticsearchStats {
    @JsonProperty
    public abstract String clusterName();

    @JsonProperty
    public abstract String clusterVersion();

    @JsonProperty
    public abstract HealthStatus status();

    @JsonProperty
    public abstract ClusterHealth clusterHealth();

    @JsonProperty
    public abstract NodesStats nodesStats();

    @JsonProperty
    public abstract IndicesStats indicesStats();

    public static ElasticsearchStats create(String clusterName,
                                            String clusterVersion,
                                            HealthStatus status,
                                            ClusterHealth clusterHealth,
                                            NodesStats nodesStats,
                                            IndicesStats indicesStats) {
        return new AutoValue_ElasticsearchStats(clusterName, clusterVersion, status, clusterHealth, nodesStats, indicesStats);
    }
}
