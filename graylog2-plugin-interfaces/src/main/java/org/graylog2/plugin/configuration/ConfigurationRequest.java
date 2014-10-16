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
package org.graylog2.plugin.configuration;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.fields.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ConfigurationRequest {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationRequest.class);

    private final Map<String, ConfigurationField> fields = Maps.newHashMap();

    public ConfigurationRequest() {}

    public void putAll(Map<String, ConfigurationField> fields) {
        this.fields.putAll(fields);
    }

    public void addField(ConfigurationField f) {
        fields.put(f.getName(), f);
    }

    public boolean containsField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public ConfigurationField getField(String fieldName) {
        return fields.get(fieldName);
    }

    public boolean removeField(String fieldName) {
        return fields.remove(fieldName) != null;
    }
    public Map<String, ConfigurationField> getFields() {
        return fields;
    }

    @JsonValue
    public Map<String, Map<String, Object>> asList() {
        final Map<String, Map<String, Object>> configs = Maps.newHashMap();

        for (ConfigurationField f : fields.values()) {
            final Map<String, Object> config = Maps.newHashMap();
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

    public void check(Configuration configuration) throws ConfigurationException {
        for (ConfigurationField field : fields.values()) {
            if (field.isOptional().equals(ConfigurationField.Optional.NOT_OPTIONAL)) {
                final String type = field.getFieldType();
                log.debug("Checking for non-optional field {} of type {} in configuration", field.getName(), type);
                switch (type) {
                    case BooleanField.FIELD_TYPE:
                        if (!configuration.booleanIsSet(field.getName())) {
                            throw new ConfigurationException("Mandatory configuration field " + field.getName() + " is missing");
                        }
                        break;
                    case NumberField.FIELD_TYPE:
                        if (!configuration.intIsSet(field.getName())) {
                            throw new ConfigurationException("Mandatory configuration field " + field.getName() + " is missing");
                        }
                        break;
                    case TextField.FIELD_TYPE:
                    case DropdownField.FIELD_TYPE:
                        if (!configuration.stringIsSet(field.getName())) {
                                throw new ConfigurationException("Mandatory configuration field " + field.getName() + " is missing");
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown field type " + type + ". This is a bug.");
                }
            }
        }
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
