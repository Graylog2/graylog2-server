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
package org.graylog2.system.information;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.cluster.Node;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *  @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AnonymousInformationCollector {
    
    private static final Logger LOG = LoggerFactory.getLogger(AnonymousInformationCollector.class);
    
    Core server;
    
    public AnonymousInformationCollector(Core server) {
        this.server = server;
    }

    public Map<String, Object> collect() {
        Map<String, Object> info = Maps.newHashMap();
        
        info.put("version", Core.GRAYLOG2_VERSION);
        info.put("number_of_loaded_modules", numberOfLoadedModules());
        info.put("number_of_elasticsearch_nodes", server.getIndexer().cluster().getNumberOfNodes());
        info.put("number_of_graylog2_server_nodes", Node.allActive(server, Node.Type.SERVER).size());
        info.put("number_of_total_messages", server.getIndexer().indices().getTotalNumberOfMessages());
        info.put("number_of_indices", server.getDeflector().getAllDeflectorIndices().size());
        info.put("number_of_streams", StreamImpl.loadAllEnabled(server).size());
        info.put("number_of_stream_rules", numberOfStreamRules());
        info.put("number_of_blacklist_rules", Blacklist.fetchAll().size());
        info.put("total_index_size", server.getIndexer().indices().getTotalSize());
        
        return info;
    }

    private Map<String, Integer> numberOfLoadedModules() {
        try {
            Map<String, Integer> plugins = Maps.newHashMap();
            plugins.put("initializers", server.initializers().count());
            plugins.put("filters", server.getFilters().size());
            plugins.put("outputs", server.outputs().count());
            plugins.put("transports", server.getTransports().size());
            plugins.put("alarm_callbacks", server.getAlarmCallbacks().size());

            return plugins;
        } catch (Exception e) {
            LOG.warn("Couldn't fetch data for loaded modules", e);
            return Maps.newHashMap();
        }
    }
    
    private int numberOfStreamRules() {
        int rules = 0;
        for (Stream stream : StreamImpl.loadAllEnabled(server)) {
            rules =+ stream.getStreamRules().size();
        }
            
        return rules;
    }

}
