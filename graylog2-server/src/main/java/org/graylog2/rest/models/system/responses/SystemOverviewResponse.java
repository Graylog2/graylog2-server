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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SystemOverviewResponse {
    @JsonProperty
    public abstract String facility();
    @JsonProperty
    public abstract String codename();
    @JsonProperty
    public abstract String nodeId();
    @JsonProperty
    public abstract String clusterId();
    @JsonProperty
    public abstract String version();
    @JsonProperty
    public abstract String startedAt();
    @JsonProperty("is_processing")
    public abstract boolean isProcessing();
    @JsonProperty
    public abstract String hostname();
    @JsonProperty
    public abstract String lifecycle();
    @JsonProperty
    public abstract String lbStatus();
    @JsonProperty
    public abstract String timezone();
    @JsonProperty("operating_system")
    public abstract String operatingSystem();

    @JsonCreator
    public static SystemOverviewResponse create(@JsonProperty("facility") String facility,
                                                @JsonProperty("codename") String codename,
                                                @JsonProperty("node_id") String nodeId,
                                                @JsonProperty("cluster_id") String clusterId,
                                                @JsonProperty("version") String version,
                                                @JsonProperty("started_at") String startedAt,
                                                @JsonProperty("is_processing") boolean isProcessing,
                                                @JsonProperty("hostname") String hostname,
                                                @JsonProperty("lifecycle") String lifecycle,
                                                @JsonProperty("lb_status") String lbStatis,
                                                @JsonProperty("timezone") String timezone,
                                                @JsonProperty("operating_system") String operatingSystem) {
        return new AutoValue_SystemOverviewResponse(facility, codename, nodeId, clusterId, version, startedAt, isProcessing, hostname, lifecycle, lbStatis, timezone, operatingSystem);
    }
}
