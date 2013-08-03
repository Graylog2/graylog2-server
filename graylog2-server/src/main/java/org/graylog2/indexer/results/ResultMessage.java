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

package org.graylog2.indexer.results;

import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ResultMessage {

	/* 
	 * I suppress all the warnings because Eclipse doesn't know shit
	 * about JSON POJO serialization.
	 */
	@SuppressWarnings("unused") public Map<String, Object> message;
	@SuppressWarnings("unused") public String index;

	protected ResultMessage() { /* use factory method */}
	
	public static ResultMessage parseFromSource(SearchHit hit) {
		ResultMessage m = new ResultMessage();
		m.setMessage(hit.getSource());
		m.setIndex(hit.getIndex());

		return m;
	}
	
	public static ResultMessage parseFromSource(GetResponse r) {
		ResultMessage m = new ResultMessage();
		m.setMessage(r.getSource());
		m.setIndex(r.getIndex());

		return m;
	}
	
	public void setMessage(Map<String, Object> message) {
		this.message = message;
	}
	
	public void setIndex(String index) {
		this.index = index;
	}

}
