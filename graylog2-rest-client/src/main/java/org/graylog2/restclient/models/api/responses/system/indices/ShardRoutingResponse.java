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
package org.graylog2.restclient.models.api.responses.system.indices;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ShardRoutingResponse {

    public int id;
    public boolean primary;
    public String state;
    public boolean active;

    @JsonProperty("relocating_to")
    public String relocatingTo;

    @JsonProperty("node_id")
    public String nodeId;

    @JsonProperty("node_hostname")
    public String nodeHostname;

    @JsonProperty("node_name")
    public String nodeName;

}
