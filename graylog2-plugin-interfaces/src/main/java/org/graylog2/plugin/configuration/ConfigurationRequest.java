/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ConfigurationRequest {

    private final List<ConfigurationField> fields;

    public ConfigurationRequest() {
        this.fields = Lists.newArrayList();
    }

    public void addField(ConfigurationField f) {
        fields.add(f);
    }

    public List<ConfigurationField> getFields() {
        return ImmutableList.copyOf(fields);
    }

    public Map<String, Map<String, Object>> asList() {
        Map<String, Map<String, Object>> configs = Maps.newHashMap();

        for (ConfigurationField f : fields) {
            Map<String, Object> config = Maps.newHashMap();
            config.put("type", f.getFieldType());
            config.put("human_name", f.getHumanName());
            config.put("description", f.getDescription());
            config.put("default_value", f.getDefaultValue());
            config.put("is_optional", f.isOptional().equals(ConfigurationField.Optional.OPTIONAL));
            config.put("attributes", f.getAttributes());
            config.put("additional_info", f.getAdditionalInformation());

            configs.put(f.getName(), config);
        }

        return configs;
    }

    public static class Templates {

        public static ConfigurationField bindAddress(String name) {
            return new TextField(
                    name,
                    "Bind address",
                    "0.0.0.0",
                    "Address to listen on. For example 0.0.0.0 or 127.0.0.1."
            );
        }

        public static ConfigurationField portNumber(String name, int port) {
            return new NumberField(
                    name,
                    "Port",
                    port,
                    "Port to listen on.",
                    NumberField.Attribute.IS_PORT_NUMBER
            );
        }

        public static ConfigurationField recvBufferSize(String name, int size) {
            return new NumberField(
                    name,
                    "Receive Buffer Size",
                    size,
                    "The size in bytes of the recvBufferSize for network connections to this input.",
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE
            );
        }

    }

}
