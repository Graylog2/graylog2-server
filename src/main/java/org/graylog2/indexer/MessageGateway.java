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

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.graylog2.Core;
import org.graylog2.plugin.streams.Stream;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageGateway {
 
    private final Core server;
    
    public MessageGateway(Core server) {
        this.server = server;
    }
    
    public int streamMessageCount(Stream stream, int sinceTimestamp) {
        CountRequestBuilder b = server.getIndexer().getClient().prepareCount();
        final QueryBuilder qb = matchQuery("streams", stream.getId().toString());
        
        b.setIndices(server.getDeflector().getAllDeflectorIndexNames());
        b.setQuery(qb);
        
        ActionFuture<CountResponse> future = server.getIndexer().getClient().count(b.request());
        return (int) future.actionGet().getCount();
    }
    
}
