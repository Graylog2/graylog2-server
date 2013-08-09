package models.api.results;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import lib.Tools;

public class MessageResult {

	private final static Set<String> HIDDEN_FIELDS = ImmutableSet.of(
			"_id",
			"timestamp",
			"streams",
            "gl2_source_input",
            "gl2_source_node"
	);
	
	private final Map<String, Object> fields;
	private final String index;
	private final String id;
	private final String timestamp;
    private final String sourceNodeId;
    private final String sourceInputId;
	
	public MessageResult(Map<String, Object> message, String index) {
		fields = Maps.newHashMap();
		for (Map.Entry<String, Object> f : message.entrySet()) {
			if (HIDDEN_FIELDS.contains(f.getKey())) {
				continue;
			}
			
			fields.put(f.getKey(), f.getValue());
		}
		
		this.id = (String) message.get("_id");
		this.timestamp = (String) message.get("timestamp");

        this.sourceNodeId = (String) message.get("gl2_source_node");
        this.sourceInputId = (String) message.get("gl2_source_input");
        this.index = index;
	}
	
	public String getId() {
		return id;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public Map<String, Object> getFields() {
		return fields;
	}

	public String getIndex() {
		return index;
	}

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getSourceInputId() {
        return sourceInputId;
    }
	
}
