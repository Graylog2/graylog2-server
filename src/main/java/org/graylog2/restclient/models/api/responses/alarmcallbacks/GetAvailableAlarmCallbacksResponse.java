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
