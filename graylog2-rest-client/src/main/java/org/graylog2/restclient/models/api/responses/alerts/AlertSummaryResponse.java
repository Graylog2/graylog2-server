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
package org.graylog2.restclient.models.api.responses.alerts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertSummaryResponse {

    public String id;
    public String description;

    @JsonProperty("stream_id")
    public String streamId;

    @JsonProperty("condition_id")
    public String conditionId;

    @JsonProperty("condition_parameters")
    public Map<String, Object> conditionParameters;

    @JsonProperty("triggered_at")
    public String triggeredAt;

}
