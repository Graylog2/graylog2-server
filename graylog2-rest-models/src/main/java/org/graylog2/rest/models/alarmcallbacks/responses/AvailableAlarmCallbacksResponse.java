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
package org.graylog2.rest.models.alarmcallbacks.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import org.graylog2.rest.models.configuration.responses.RequestedConfigurationField;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AvailableAlarmCallbacksResponse {
    public Map<String, AvailableAlarmCallbackSummaryResponse> types;

    @JsonIgnore
    public Map<String, List<RequestedConfigurationField>> getRequestedConfiguration() {
        Map<String, List<RequestedConfigurationField>> result = Maps.newHashMap();

        for (Map.Entry<String, AvailableAlarmCallbackSummaryResponse> entry : types.entrySet()) {
            result.put(entry.getKey(), entry.getValue().extractRequestedConfiguration(entry.getValue().requested_configuration));
        }

        return result;
    }
}
