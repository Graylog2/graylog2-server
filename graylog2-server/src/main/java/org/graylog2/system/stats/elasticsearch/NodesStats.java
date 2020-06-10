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

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class NodesStats {
    @JsonProperty
    public abstract int total();

    @JsonProperty
    public abstract int parentOnly();

    @JsonProperty
    public abstract int dataOnly();

    @JsonProperty
    public abstract int parentData();

    @JsonProperty
    public abstract int client();

    public static NodesStats create(int total,
                                   int parentOnly,
                                   int dataOnly,
                                   int parentData,
                                   int client) {
        return new AutoValue_NodesStats(total, parentOnly, dataOnly, parentData, client);
    }
}
