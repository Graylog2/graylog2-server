package org.graylog2.restclient.models;

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.GetSingleAvailableAlarmCallbackResponse;

import java.util.List;
import java.util.Map;

public abstract class ConfigurableEntity {
    public abstract Map<String, Object> getConfiguration();

    public Map<String, Object> getConfiguration(List<RequestedConfigurationField> typeResponses) {
        final Map<String, Object> result = Maps.newHashMapWithExpectedSize(getConfiguration().size());

        for (final RequestedConfigurationField configurationField : typeResponses) {
            if (configurationField.getAttributes().contains("is_password")) {
                result.put(configurationField.getTitle(), "*******");
            } else {
                result.put(configurationField.getTitle(), getConfiguration().get(configurationField.getTitle()));
            }
        }
        return result;
    }
}
