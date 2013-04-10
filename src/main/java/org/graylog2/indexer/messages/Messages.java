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

package org.graylog2.indexer.messages;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.Core;
import org.graylog2.indexer.results.ResultMessage;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Messages {
	
	@SuppressWarnings("unused")
	private final Core server;
	private final Client c;
	
	public Messages(Client client, Core server) {
		this.server = server;
		this.c = client;
	}
	
	public ResultMessage get(String messageId, String index) throws IndexMissingException, DocumentNotFoundException {
		GetRequestBuilder grb = new GetRequestBuilder(c, index);
		grb.setId(messageId);

		GetResponse r = c.get(grb.request()).actionGet();
		
		if (!r.exists()) {
			throw new DocumentNotFoundException();
		}
		
		return ResultMessage.parseFromSource(r);
	}
	
}
