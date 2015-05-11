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
package org.graylog2.collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import org.graylog2.database.CollectionName;
import org.graylog2.rest.models.collector.responses.CollectorSummary;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
@CollectionName("collectors")
public abstract class CollectorImpl implements Collector {

    @JsonProperty("id")
    @Override
    public abstract String getId();

    @JsonProperty("node_id")
    @Override
    public abstract String getNodeId();

    @JsonProperty("node_details")
    public abstract CollectorNodeDetails getNodeDetails();

    @JsonProperty("collector_version")
    public abstract String getCollectorVersion();

    @Override
    public CollectorSummary toSummary(Function<Collector, Boolean> isActiveFunction) {
        final Boolean isActive = isActiveFunction.apply(this);
        return CollectorSummary.create(getId(), getNodeId(), getNodeDetails().toSummary(),
                getLastSeen(), getCollectorVersion(), isActive != null && isActive);
    }

    @JsonProperty("last_seen")
    @Override
    public abstract DateTime getLastSeen();

    @JsonCreator
    public static CollectorImpl create(@JsonProperty("_id") String objectId,
                                   @JsonProperty("id") String id,
                                   @JsonProperty("node_id") String nodeId,
                                   @JsonProperty("node_details") CollectorNodeDetails collectorNodeDetails,
                                   @JsonProperty("collector_version") String collectorVersion,
                                   @JsonProperty("last_seen") DateTime lastSeen) {
        return new AutoValue_CollectorImpl(id, nodeId, collectorNodeDetails, collectorVersion, lastSeen);
    }

    public static CollectorImpl create(String collectorId, String nodeId, String collectorVersion, CollectorNodeDetails collectorNodeDetails, DateTime lastSeen) {
        return new AutoValue_CollectorImpl(collectorId, nodeId, collectorNodeDetails, collectorVersion, lastSeen);
    }
}
