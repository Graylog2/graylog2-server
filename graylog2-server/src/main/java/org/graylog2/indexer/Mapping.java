/**
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
package org.graylog2.indexer;

import com.google.common.collect.Maps;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.client.Client;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Tools;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Representing the message type mapping in ElasticSearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@SuppressWarnings({"unchecked"})
public class Mapping {

    public static PutMappingRequest getPutMappingRequest(final Client client, final String index, final String analyzer) {
        final PutMappingRequestBuilder builder = client.admin().indices().preparePutMapping(new String[] {index});
        builder.setType(Messages.TYPE);

        final Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("properties", partFieldProperties(analyzer));
        mapping.put("dynamic_templates", partDefaultAllInDynamicTemplate());
        mapping.put("_source", enabledAndCompressed()); // Compress source field..
        mapping.put("_ttl", enabled()); // Enable purging by TTL.

        // TODO: use multimap?
        final Map<String, Map<String, Object>> completeMapping = Maps.newHashMap();
        completeMapping.put(Messages.TYPE, mapping);

        builder.setSource(completeMapping);
        return builder.request();
    }

    /*
     * Disable analyzing for every field by default.
     */
    // TODO warnings
    @SuppressWarnings("rawtypes")
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
    // TODO warnings
    @SuppressWarnings("rawtypes")
	private static Map partFieldProperties(String analyzer) {
        final Map<String, Map<?, ?>> properties = Maps.newHashMap();

        properties.put("message", analyzedString(analyzer));
        properties.put("full_message", analyzedString(analyzer));

        // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
        // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
        properties.put("timestamp", typeTimeWithMillis());

        // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
        properties.put("source", analyzedString("analyzer_keyword"));

        return properties;
    }

    private static Map<String, String> analyzedString(String analyzer) {
        final Map<String, String> type = Maps.newHashMap();
        type.put("index", "analyzed");
        type.put("type", "string");
        type.put("analyzer", analyzer);

        return type;
    }

    private static Map<String, String> typeTimeWithMillis() {
        final Map<String, String> type = Maps.newHashMap();
        type.put("type", "date");
        type.put("format", Tools.ES_DATE_FORMAT);

        return type;
    }

    private static Map<String, Boolean> enabled() {
        final Map<String, Boolean> e = Maps.newHashMap();
        e.put("enabled", true);

        return e;
    }

    
    private static Map<String, Boolean> enabledAndCompressed() {
        final Map<String, Boolean> e = Maps.newHashMap();
        e.put("enabled", true);
        e.put("compress", true);

        return e;
    }

}