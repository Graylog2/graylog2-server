/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package models.api.responses;

import com.google.gson.annotations.SerializedName;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodeSummaryResponse {

    @SerializedName("id")
    public String nodeId;

    @SerializedName("short_node_id")
    public String shortNodeId;

    public String hostname;

    @SerializedName("last_seen")
    public String lastSeen;

    @SerializedName("transport_address")
    public String transportAddress;

    @SerializedName("is_master")
    public boolean isMaster;

    @SerializedName("is_processing")
    public boolean isProcessing;

}
