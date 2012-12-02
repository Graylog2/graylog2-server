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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.graylog2.Core;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginRegistry {
    
    public static void setActiveTransports(Core server, List<Transport> transports) {
        Set<Map<String, Object>> r = Sets.newHashSet();
        
        for(Transport transport : transports) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("typeclass", transport.getClass().getCanonicalName());
            entry.put("name", transport.getName());
            
            r.add(entry);
        }
        
        server.getMongoBridge().writePluginInformation(r, "transports");
    }
    
    public static void setActiveAlarmCallbacks(Core server, List<AlarmCallback> callbacks) {
        Set<Map<String, Object>> r = Sets.newHashSet();
        
        for(AlarmCallback callback : callbacks) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("typeclass", callback.getClass().getCanonicalName());
            entry.put("name", callback.getName());
            entry.put("requested_config", callback.getRequestedConfiguration());
            
            r.add(entry);
        }
        
        server.getMongoBridge().writePluginInformation(r, "alarm_callbacks");
    }
    
    public static void setActiveMessageOutputs(Core server, List<MessageOutput> outputs) {
        Set<Map<String, Object>> r = Sets.newHashSet();
        
        for(MessageOutput output : outputs) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("typeclass", output.getClass().getCanonicalName());
            entry.put("name", output.getName());
            entry.put("requested_config", output.getRequestedConfiguration());
            entry.put("requested_stream_config", output.getRequestedStreamConfiguration());
            
            r.add(entry);
        }
        
        server.getMongoBridge().writePluginInformation(r, "message_outputs");
    }
    
    public static void setActiveMessageInputs(Core server, List<MessageInput> inputs) {
        Set<Map<String, Object>> r = Sets.newHashSet();
        
        for(MessageInput input : inputs) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("typeclass", input.getClass().getCanonicalName());
            entry.put("name", input.getName());
            entry.put("requested_config", input.getRequestedConfiguration());
            
            r.add(entry);
        }
        
        server.getMongoBridge().writePluginInformation(r, "message_inputs");
    }
    
    public static void setActiveInitializers(Core server, List<Initializer> initializers) {
        Set<Map<String, Object>> r = Sets.newHashSet();
        
        for(Initializer initializer : initializers) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("typeclass", initializer.getClass().getCanonicalName());
            entry.put("name", initializer.getName());
            entry.put("requested_config", initializer.getRequestedConfiguration());
            
            r.add(entry);
        }
        
        server.getMongoBridge().writePluginInformation(r, "initializers");
    }
    
}
