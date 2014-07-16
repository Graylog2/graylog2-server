/*
 * Copyright 2012-2014 TORCH GmbH
 *
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
