/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.responses;

import com.google.common.collect.Lists;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("requested_configuration")
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