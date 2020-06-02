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

@AutoValue
@JsonAutoDetect
public abstract class ShardStats {
    @JsonProperty
    public abstract int numberOfNodes();

    @JsonProperty
    public abstract int numberOfDataNodes();

    @JsonProperty
    public abstract int activeShards();

    @JsonProperty
    public abstract int relocatingShards();

    @JsonProperty
    public abstract int activePrimaryShards();

    @JsonProperty
    public abstract int initializingShards();

    @JsonProperty
    public abstract int unassignedShards();

    @JsonProperty
    public abstract boolean timedOut();

    public static ShardStats create(int numberOfNodes,
                                    int numberOfDataNodes,
                                    int activeShards,
                                    int relocatingShards,
                                    int activePrimaryShards,
                                    int initializingShards,
                                    int unassignedShards,
                                    boolean timedOut) {
        return new AutoValue_ShardStats(numberOfNodes, numberOfDataNodes, activeShards, relocatingShards,
                activePrimaryShards, initializingShards, unassignedShards, timedOut);
    }
}
