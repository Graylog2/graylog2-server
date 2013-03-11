/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.indexer.searches;

import java.util.Map;

import org.elasticsearch.search.SearchHit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SearchResultMessage {

	/* 
	 * I suppress all the warnings because Eclipse doesn't know shit
	 * about JSON POJO serialization.
	 */
	@SuppressWarnings("unused") private Map<String, Object> message;
	@SuppressWarnings("unused") private String index;
	@SuppressWarnings("unused") private String nodeId;
	@SuppressWarnings("unused") private int shardId;

	protected SearchResultMessage() { /* use factory method */}
	
	public static SearchResultMessage parseFromSource(SearchHit hit) {
		SearchResultMessage m = new SearchResultMessage();
		m.setMessage(hit.getSource());
		m.setIndex(hit.getIndex());
		m.setNodeId(hit.getShard().getNodeId());
		m.setShardId(hit.getShard().getShardId());
		
		return m;
	}
	
	public void setMessage(Map<String, Object> message) {
		this.message = message;
	}
	
	public void setIndex(String index) {
		this.index = index;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	public void setShardId(int shardId) {
		this.shardId = shardId;
	}

}
