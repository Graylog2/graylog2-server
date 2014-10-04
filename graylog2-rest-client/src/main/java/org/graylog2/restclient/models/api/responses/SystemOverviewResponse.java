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
package org.graylog2.restclient.models.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemOverviewResponse {

    public String facility;
    public String version;
    public String codename;

    @JsonProperty("server_id")
    public String serverId;

    @JsonProperty("started_at")
    public String startedAt;

    public String hostname;

    public String lifecycle;

    @JsonProperty("is_processing")
    public boolean isProcessing;

    @JsonProperty("lb_status")
    public String lbStatus;

    public String timezone;

    public static SystemOverviewResponse buildEmpty() {
        final SystemOverviewResponse response = new SystemOverviewResponse();

        response.hostname = "unknown";
        response.version = "unknown";
        response.lifecycle = "unknown";
        response.lbStatus = "unknown";

        return response;
    }
}
