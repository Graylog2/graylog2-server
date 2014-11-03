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
package org.graylog2.restclient.models.api.responses.system;

import com.google.gson.annotations.SerializedName;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobSummaryResponse {

    public String id;
    public String description;
    public String name;

    @SerializedName("node_id")
    public String nodeId;

    @SerializedName("started_at")
    public String startedAt;

    @SerializedName("started_by")
    public String startedBy;

    @SerializedName("percent_complete")
    public int percentComplete;

    @SerializedName("is_cancelable")
    public boolean isCancelable;

    @SerializedName("provides_progress")
    public boolean providesProgress;
}
