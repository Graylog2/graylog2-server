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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
public abstract class ShardRouting {
    @JsonProperty
    public abstract int id();

    @JsonProperty
    public abstract String state();

    @JsonProperty
    public abstract boolean active();

    @JsonProperty
    public abstract boolean primary();

    @JsonProperty
    public abstract String nodeId();

    @JsonProperty
    public abstract String nodeName();

    @JsonProperty
    public abstract String nodeHostname();

    @JsonProperty
    @Nullable
    public abstract String relocatingTo();

    public static ShardRouting create(int id,
                                      String state,
                                      boolean active,
                                      boolean primary,
                                      String nodeId,
                                      String nodeName,
                                      String nodeHostname,
                                      @Nullable String relocatingTo) {
        return new AutoValue_ShardRouting(id, state, active, primary, nodeId, nodeName, nodeHostname, relocatingTo);
    }
}
