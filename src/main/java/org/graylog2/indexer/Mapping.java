/**
 * Copyright 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.client.Client;

/**
 * Representing the message type mapping in ElasticSearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Mapping {

    public static PutMappingRequest getPutMappingRequest(final Client client, final String index) {
        final PutMappingRequestBuilder builder = client.admin().indices().preparePutMapping(new String[] {index});
        builder.setType(EmbeddedElasticSearchClient.TYPE);

        final Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("properties", partFieldProperties());
        mapping.put("dynamic_templates", partDefaultAllInDynamicTemplate());
        mapping.put("_source", enabledAndCompressed()); // Compress source field..

        final Map completeMapping = new HashMap();
        completeMapping.put(EmbeddedElasticSearchClient.TYPE, mapping);

        builder.setSource(completeMapping);
        return builder.request();
    }

    public static Map get() {
        final Map mapping = new HashMap();
        mapping.put("properties", partFieldProperties());
        mapping.put("dynamic_templates", partDefaultAllInDynamicTemplate());
        mapping.put("_source", enabledAndCompressed()); // Compress source field..

        final Map completeMapping = new HashMap();
        completeMapping.put(EmbeddedElasticSearchClient.TYPE, mapping);

        final Map spec = new HashMap();
        spec.put("mappings", completeMapping);

        return spec;
    }

    /*
     * Disable analyzing for every field by default.
     */
    private static List partDefaultAllInDynamicTemplate() {
        final List dynamicTemplates = new LinkedList();
        final Map template = new HashMap();
        final Map defaultAll = new HashMap();
        final Map notAnalyzed = new HashMap();
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
        final Map properties = new HashMap();

        properties.put("message", analyzedString());
        properties.put("full_message", analyzedString());

        // Required for the WI to not fail on empty indexes.
        properties.put("created_at", typeNumberDouble());

        // This is used building histograms. An own field to avoid mapping problems with oder versions.
        properties.put("histogram_time", typeTimeNoMillis()); // yyyy-MM-dd HH-mm-ss

        return properties;
    }

    private static Map analyzedString() {
        final Map type = new HashMap();
        type.put("index", "analyzed");
        type.put("type", "string");
        type.put("analyzer", "whitespace");

        return type;
    }

    private static Map typeNumberDouble() {
        final Map type = new HashMap();
        type.put("type", "double");

        return type;
    }

    private static Map typeTimeNoMillis() {
        final Map type = new HashMap();
        type.put("type", "date");
        type.put("format", "yyyy-MM-dd HH-mm-ss");

        return type;
    }

    private static Map enabledAndCompressed() {
        final Map e = new HashMap();
        e.put("enabled", true);
        e.put("compress", true);

        return e;
    }

}