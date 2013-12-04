/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@torch.sh>
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

package org.graylog2.plugin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Message {

    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    private Map<String, Object> fields = Maps.newHashMap();
    private List<Stream> streams = Lists.newArrayList();

    private MessageInput sourceInput;

    private static final Pattern ALPHANUMERIC_ASCII_AND_UNDERSCORE_AND_DOTS = Pattern.compile("^[\\w\\.]*$");

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
        "timestamp",
        "gl2_source_node",
        "gl2_source_input",
        "gl2_source_radio",
        "gl2_source_radio_input"
    );

    public static final ImmutableSet<String> RESERVED_SETTABLE_FIELDS = ImmutableSet.of(
            "message",
            "source",
            "timestamp",
            "gl2_source_node",
            "gl2_source_input",
            "gl2_source_radio",
            "gl2_source_radio_input"
    );

    private static final ImmutableSet<String> REQUIRED_FIELDS = ImmutableSet.of(
            "message", "source", "_id"
    );

    public Message(String message, String source, DateTime timestamp) {
    	// Adding the fields directly because they would not be accepted as a reserved fields.
        fields.put("_id", new com.eaio.uuid.UUID().toString());
        fields.put("message", message);
        fields.put("source", source);
        fields.put("timestamp", timestamp);
    }

    public Message(Map<String, Object> fields) {
        this.fields.putAll(fields);
    }

    public boolean isComplete() {
    	for (String key : REQUIRED_FIELDS) {
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
        obj.put("timestamp", Tools.buildElasticSearchTimeFormat((DateTime) getField("timestamp")));

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
        if (value == null) {
            return;
        }

        if (String.class.equals(value.getClass())) {
            value = ((String) value).trim();
        }

        // Don't accept protected keys. (some are allowed though lol)
        if (RESERVED_FIELDS.contains(key) && !RESERVED_SETTABLE_FIELDS.contains(key)) {
            return;
        }

        // Only allow alphanumeric characters (and underscores)
        if(!validKey(key)) {
            return;
        }

        this.fields.put(key.trim(), value);
    }

    public static boolean validKey(String key) {
        return ALPHANUMERIC_ASCII_AND_UNDERSCORE_AND_DOTS.matcher(key).matches();
    }

    public void addFields(Map<String, Object> fields) {
        if(fields == null) {
            return;
        }

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void addStringFields(Map<String, String> fields) {
        if(fields == null) {
            return;
        }

        for (Map.Entry<String, String> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void addIntegerFields(Map<String, Integer> fields) {
        if(fields == null) {
            return;
        }

        for (Map.Entry<String, Integer> field : fields.entrySet()) {
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

    public MessageInput getSourceInput() {
        return sourceInput;
    }

    public void setSourceInput(MessageInput input) {
        this.sourceInput = input;
    }

}
