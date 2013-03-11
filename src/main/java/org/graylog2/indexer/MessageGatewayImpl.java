/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.plugin.indexer.MessageGateway;
import org.elasticsearch.index.query.QueryBuilder;
import org.graylog2.Core;

import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageGatewayImpl implements MessageGateway {
 
    private final Core server;
    
    public MessageGatewayImpl(Core server) {
        this.server = server;
    }
    
    @Override
    public int streamMessageCount(String streamId, int sinceTimestamp) {
        final QueryBuilder qb = filteredQuery(
                matchQuery("streams", streamId),
                rangeFilter("created_at").gte(sinceTimestamp)
        );
        
        return countOnAllIndices(qb);
    }

    @Override
    public int totalMessageCount(int sinceTimestamp) {
        final QueryBuilder qb = filteredQuery(
                matchAllQuery(),
                rangeFilter("created_at").gte(sinceTimestamp)
        );

        return countOnAllIndices(qb);
    }
    
    private int countOnAllIndices(QueryBuilder qb) {
       CountRequestBuilder b = server.getIndexer().getClient().prepareCount();
        
        b.setIndices(server.getDeflector().getAllDeflectorIndexNames());
        b.setQuery(qb);
        
        ActionFuture<CountResponse> future = server.getIndexer().getClient().count(b.request());
        return (int) future.actionGet().getCount();
    }
    
}
