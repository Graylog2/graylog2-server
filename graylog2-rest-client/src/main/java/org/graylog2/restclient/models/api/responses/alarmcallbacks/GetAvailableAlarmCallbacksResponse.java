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
package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GetAvailableAlarmCallbacksResponse {
    public Map<String, GetSingleAvailableAlarmCallbackResponse> types;

    public Map<String, List<RequestedConfigurationField>> getRequestedConfiguration() {
        Map<String, List<RequestedConfigurationField>> result = Maps.newHashMap();

        for (Map.Entry<String, GetSingleAvailableAlarmCallbackResponse> entry : types.entrySet()) {
            result.put(entry.getKey(), entry.getValue().extractRequestedConfiguration(entry.getValue().requested_configuration));
        }

        return result;
    }
}
