/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.indexer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mapping.java: Sep 05, 2011 3:34:57 PM
 *
 * Representing the message type mapping in ElasticSearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Mapping {
    
    private static String TYPE = "message";

    public static Map<String, Object> get() {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("properties", partFieldProperties());
        mapping.put("dynamic_templates", partDefaultAllInDynamicTemplate());

        Map<String, Map> completeMapping = new HashMap<String, Map>();
        completeMapping.put(TYPE, mapping);

        Map<String, Object> spec = new HashMap<String, Object>();
        spec.put("mappings", completeMapping);

        return spec;
    }

    /*
     * Disable analyzing for every field by default.
     */
    private static List<Map> partDefaultAllInDynamicTemplate() {
        List<Map> dynamicTemplates = new LinkedList<Map>();
        Map<String, Map> template = new HashMap<String, Map>();
        Map<String, Object> defaultAll = new HashMap<String, Object>();
        Map<String, String> notAnalyzed = new HashMap<String, String>();
        notAnalyzed.put("index", "not_analyzed");

        // Match all.
        defaultAll.put("match", "*");
        // Analyze nothing by default.
        defaultAll.put("mapping", notAnalyzed);

        template.put("store_generic", defaultAll);
        dynamicTemplates.add(template);

        return dynamicTemplates;
    }

    /*
     * Enable analyzing for some fields again. Like for message and full_message.
     */
    private static Map partFieldProperties() {
        Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

        properties.put("message", analyzedString());
        properties.put("full_message", analyzedString());

        // Required for the WI to not fail on empty indexes.
        properties.put("created_at", typeNumberDouble());

        return properties;
    }

    private static Map<String, String> analyzedString() {
        Map<String, String> type = new HashMap<String, String>();
        type.put("index", "analyzed");
        type.put("type", "string");
        type.put("analyzer", "whitespace");
        
        return type;
    }

    private static Map<String, String> typeNumberDouble() {
        Map<String, String> type = new HashMap<String, String>();
        type.put("type", "double");

        return type;
    }

}