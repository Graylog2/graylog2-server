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
package org.graylog2.indexer.indices;

import com.google.auto.value.AutoValue;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@AutoValue
@WithBeanGetter
public abstract class IndexStatistics {
    public abstract String indexName();

    public abstract CommonStats primaries();

    public abstract CommonStats total();

    public abstract List<ShardRouting> shardRoutings();

    public static IndexStatistics create(String indexName,
                                         CommonStats primaries,
                                         CommonStats total,
                                         List<ShardRouting> shardRoutings) {
        return new AutoValue_IndexStatistics(indexName, primaries, total, shardRoutings);
    }
}
