package org.graylog2.restclient.models.api.responses;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import org.graylog2.restclient.lib.plugin.configuration.BooleanField;
import org.graylog2.restclient.lib.plugin.configuration.DropdownField;
import org.graylog2.restclient.lib.plugin.configuration.NumberField;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;
import org.graylog2.restclient.lib.plugin.configuration.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AvailableOutputSummary {
    private static final Logger LOG = LoggerFactory.getLogger(AvailableOutputSummary.class);

    public String name;
    @SerializedName("requested_configuration")
    public Map<String, Map<String, Object>> requestedConfiguration;

    public List<RequestedConfigurationField> getRequestedConfiguration() {
        List<RequestedConfigurationField> fields = Lists.newArrayList();
        List<RequestedConfigurationField> tmpBools = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> c : requestedConfiguration.entrySet()) {
            try {
                String fieldType = (String) c.getValue().get("type");
                switch(fieldType) {
                    case "text":
                        fields.add(new TextField(c));
                        continue;
                    case "number":
                        fields.add(new NumberField(c));
                        continue;
                    case "boolean":
                        tmpBools.add(new BooleanField(c));
                        continue;
                    case "dropdown":
                        fields.add(new DropdownField(c));
                        continue;
                    default:
                        LOG.info("Unknown field type [{}].", fieldType);
                }
            } catch (Exception e) {
                LOG.error("Skipping invalid configuration field [" + c.getKey() + "]", e);
            }
        }

        // We want the boolean fields at the end for display/layout reasons.
        fields.addAll(tmpBools);

        return fields;
    }
}