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
package org.graylog2.indexer.indices;

import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.cluster.routing.ShardRouting;

import java.util.List;

public class IndexStatistics {
    private CommonStats primaries;
    private CommonStats total;

    private List<ShardRouting> shardRoutingList;

    public IndexStatistics() {
        shardRoutingList = Lists.newArrayList();
    }

    public void setPrimaries(CommonStats primaries) {
        this.primaries = primaries;
    }

    public CommonStats getPrimaries() {
        return primaries;
    }

    public void setTotal(CommonStats total) {
        this.total = total;
    }

    public CommonStats getTotal() {
        return total;
    }

    public void addShardRouting(ShardRouting shardRouting) {
        shardRoutingList.add(shardRouting);
    }

    public List<ShardRouting> getShardRoutings() {
        return shardRoutingList;
    }
}
