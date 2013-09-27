/*
 * Copyright 2013 TORCH UG
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
