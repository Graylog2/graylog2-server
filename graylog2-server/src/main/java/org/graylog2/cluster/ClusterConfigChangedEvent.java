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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterConfigChangedEvent {
    @JsonProperty
    public abstract DateTime date();

    @JsonProperty
    @NotEmpty
    public abstract String nodeId();

    @JsonProperty
    @NotEmpty
    public abstract String type();

    @JsonCreator
    public static ClusterConfigChangedEvent create(@JsonProperty("date") DateTime date,
                                                   @JsonProperty("node_id") @NotEmpty String nodeId,
                                                   @JsonProperty("type") @NotEmpty String type) {
        return new AutoValue_ClusterConfigChangedEvent(date, nodeId, type);
    }
}
