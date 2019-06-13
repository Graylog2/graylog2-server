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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = Interval.TYPE_FIELD,
        defaultImpl = TimeUnitInterval.class)
@JsonAutoDetect
public interface Interval {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    DateHistogramInterval toDateHistogramInterval(TimeRange timerange);
}
