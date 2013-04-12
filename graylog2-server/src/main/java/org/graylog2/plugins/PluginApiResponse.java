/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */
package org.graylog2.plugins;

import java.util.Set;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginApiResponse {
    
    String shortname;
    String version;
    String name;
    int plugin_type;
    String jar;
    Set<String> compatible_versions;
    
    public String getPluginTypeName() {
        switch (plugin_type) {
            case 0:
                return "initializers";
            case 1:
                return "inputs";
            case 2:
                return "filters";
            case 3:
                return "outputs";
            case 4:
                return "alarm_transports";
            case 5:
                return "alarm_callbacks";
        }
        
        return "unknown";
    }
    
    public Class<?> getClassOfPlugin() {
        switch (plugin_type) {
            case 0:
                return Initializer.class;
            case 1:
                return MessageInput.class;
            case 2:
                return MessageFilter.class;
            case 3:
                return MessageOutput.class;
            case 4:
                return Transport.class;
            case 5:
                return AlarmCallback.class;
        }
        
        throw new RuntimeException("Unknown plugin type <" + plugin_type + ">.");
    }
    
    public String getRegistryName() {
        switch (plugin_type) {
            case 0:
                return "initializers";
            case 1:
                return "message_inputs";
            case 2:
                return "message_filters";
            case 3:
                return "message_outputs";
            case 4:
                return "transports";
            case 5:
                return "alarm_callbacks";
        }
        
        throw new RuntimeException("Unknown plugin type <" + plugin_type + ">.");
    }
    
}
