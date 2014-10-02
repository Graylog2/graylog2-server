/*
 * Copyright 2013 TORCH UG
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
 */
package org.graylog2.restclient.models.api.responses.system;

import com.google.gson.annotations.SerializedName;
import org.graylog2.restclient.models.api.responses.ByteListing;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ClusterEntityJVMStatsResponse {

    @SerializedName("node_id")
    public String nodeId;

    public String info;
    public String pid;

    @SerializedName("max_memory")
    public ByteListing maxMemory;

    @SerializedName("used_memory")
    public ByteListing usedMemory;

    @SerializedName("total_memory")
    public ByteListing totalMemory;

    @SerializedName("free_memory")
    public ByteListing freeMemory;

}
