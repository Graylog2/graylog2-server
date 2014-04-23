package models.api.responses.alarmcallbacks;

import com.google.common.collect.Lists;
import lib.plugin.configuration.*;
import play.Logger;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GetSingleAvailableAlarmCallbackResponse {
    public String name;
    public Map<String, Map<String, Object>> requested_configuration;

    public List<RequestedConfigurationField> getRequestedConfiguration() {
        return extractRequestedConfiguration(requested_configuration);
    }

    public List<RequestedConfigurationField> extractRequestedConfiguration(Map<String, Map<String, Object>> config) {
        List<RequestedConfigurationField> result = Lists.newArrayList();
        List<RequestedConfigurationField> booleanFields = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> entry : config.entrySet()) {
            try {
                String fieldType = (String) entry.getValue().get("type");
                switch(fieldType) {
                    case "text":
                        result.add(new TextField(entry));
                        continue;
                    case "number":
                        result.add(new NumberField(entry));
                        continue;
                    case "boolean":
                        booleanFields.add(new BooleanField(entry));
                        continue;
                    case "dropdown":
                        result.add(new DropdownField(entry));
                        continue;
                    default:
                        Logger.info("Unknown field type [" + fieldType + "].");
                }
            } catch (Exception e) {
                Logger.error("Skipping invalid configuration field [" + entry.getKey() + "]", e);
            }
        }

        result.addAll(booleanFields);

        return result;
    }
}
