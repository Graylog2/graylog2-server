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

import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SystemJVMResponse {
    @JsonProperty
    public abstract Map<String, Long> freeMemory();
    @JsonProperty
    public abstract Map<String, Long> maxMemory();
    @JsonProperty
    public abstract Map<String, Long> totalMemory();
    @JsonProperty
    public abstract Map<String, Long> usedMemory();
    @JsonProperty
    public abstract String nodeId();
    @JsonProperty
    public abstract String pid();
    @JsonProperty
    public abstract String info();

    @JsonCreator
    public static SystemJVMResponse create(@JsonProperty("free_memory") Map<String, Long> freeMemory,
                                           @JsonProperty("max_memory") Map<String, Long> maxMemory,
                                           @JsonProperty("total_memory") Map<String, Long> totalMemory,
                                           @JsonProperty("used_memory") Map<String, Long> usedMemory,
                                           @JsonProperty("node_id") String nodeId,
                                           @JsonProperty("pid") String pid,
                                           @JsonProperty("info") String info) {
        return new AutoValue_SystemJVMResponse(freeMemory, maxMemory, totalMemory, usedMemory, nodeId, pid, info);
    }
}
