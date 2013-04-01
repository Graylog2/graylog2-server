/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.plugin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import org.graylog2.plugin.streams.Stream;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Message {
	
    private Map<String, Object> fields = Maps.newHashMap();
    private List<Stream> streams = Lists.newArrayList();
    
    // Used for drools to filter out messages.
    private boolean filterOut = false;
    
    public static final ImmutableSet<String> RESERVED_FIELDS = ImmutableSet.of(
        // ElasticSearch fields.
        "_id",
        "_ttl",
        "_source",
        "_all",
        "_index",
        "_type",
        "_score",
        
        // Our reserved fields.
        "message",
        "source",
        "timestamp"
    );

    public Message(String message, String source, double timestamp) {
    	// Adding the fields directly because they would not be accepted as a reserved fields.
        fields.put("_id", new com.eaio.uuid.UUID().toString());
        fields.put("message", message);
        fields.put("source", source);
        fields.put("timestamp", timestamp);
    }

    public boolean isComplete() {
    	final ImmutableSet<String> required = ImmutableSet.of(
    			"message", "source", "_id"
        );
    	
    	for (String key : required) {
    		if (getField(key) == null || ((String) getField(key)).isEmpty()) {
    			return false;
    		}
    	}
    	
        return true;
    }

    public String getId() {
        return (String) getField("_id");
    }

    public Map<String, Object> toElasticSearchObject() {
        Map<String, Object> obj = Maps.newHashMap();
        
        // Standard fields.
        obj.put("message", getMessage());
        obj.put("source", this.getSource());

        // Add fields.
        obj.putAll(getFields());
        
        // Timestamp
        obj.put("timestamp", Tools.buildElasticSearchTimeFormat((Double) getField("timestamp")));

        // Manually converting stream ID to string - caused strange problems without it.
        if (getStreams().size() > 0) {
            List<String> streamIds = Lists.newArrayList();
            for (Stream stream : this.getStreams()) {
                streamIds.add(stream.getId().toString());
            }
            obj.put("streams", streamIds);
        } else {
            obj.put("streams", Collections.EMPTY_LIST);
        }

        return obj;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("source: ").append(getField("source")).append(" | ");
        sb.append("message: ").append(getField("message"));
        
        // Replace all newlines and tabs.
        String ret = sb.toString().replaceAll("\\n", "").replaceAll("\\t", "");

        // Cut to 225 chars if the message is too long.
        if (ret.length() > 225) {
            ret = ret.substring(0, 225);
            ret += " (...)";
        }

        return ret;
    }
    
    public String getMessage() {
        return (String) getField("message");
    }

    public String getSource() {
        return (String) getField("source");
    }
    
    public void addField(String key, Object value) {
        // Don't accept protected keys.
        if (RESERVED_FIELDS.contains(key)) {
            return;
        }
        
        this.fields.put(key.trim(), value);
    }      

    public void addFields(Map<String, String> fields) {
        for (Map.Entry<String, String> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void removeField(String key) {
    	if (!RESERVED_FIELDS.contains(key)) {
    		this.fields.remove(key);
    	}
    }
    
    public Object getField(String key) {
    	return fields.get(key);
    }

    public Map<String, Object> getFields() {
        return this.fields;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    public List<Stream> getStreams() {
        return this.streams;
    }
    
    public void setFilterOut(boolean set) {
        this.filterOut = set;
    }
    
    public boolean getFilterOut() {
        return this.filterOut;
    }

}
