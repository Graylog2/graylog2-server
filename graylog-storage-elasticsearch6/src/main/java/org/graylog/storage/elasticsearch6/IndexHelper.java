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
package org.graylog.storage.elasticsearch6;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;

public class IndexHelper {
    @Nullable
    public static QueryBuilder getTimestampRangeFilter(TimeRange range) {
        if (range == null) {
            return null;
        }

        return QueryBuilders.rangeQuery(Message.FIELD_TIMESTAMP)
                .gte(Tools.buildElasticSearchTimeFormat(range.getFrom()))
                .lte(Tools.buildElasticSearchTimeFormat(range.getTo()));
    }
}
