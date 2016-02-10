/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.elasticsearch;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.TransportRequestOptions;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class GlobalTimeoutClient extends FilterClient {
    private final long timeout;
    private final TimeUnit unit;
    private final Counter searchRequests;

    public GlobalTimeoutClient(Client in, long timeout, TimeUnit unit, MetricRegistry metricRegistry) {
        super(in);

        checkArgument(timeout > 0);
        this.timeout = timeout;
        this.unit = checkNotNull(unit);
        this.searchRequests = metricRegistry.counter(name(GlobalTimeoutClient.class, "search-requests"));
    }

    @Override
    protected <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {
        super.doExecute(new GlobalTimeoutAction<>(action, timeout, unit), request, listener);
    }

    @Override
    public ActionFuture<SearchResponse> search(SearchRequest request) {
        searchRequests.inc();
        return super.search(request);
    }

    @Override
    public void search(final SearchRequest request, final ActionListener<SearchResponse> listener) {
        searchRequests.inc();
        super.search(request, listener);
    }

    public static class GlobalTimeoutAction<Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>>
            extends Action<Request, Response, RequestBuilder> {
        private final Action<Request, Response, RequestBuilder> action;
        private final TimeValue timeout;

        public GlobalTimeoutAction(final Action<Request, Response, RequestBuilder> action, long duration, TimeUnit timeUnit) {
            super(action.name());
            this.action = checkNotNull(action);
            this.timeout = new TimeValue(duration, timeUnit);
        }

        @Override
        public TransportRequestOptions transportOptions(Settings settings) {
            final TransportRequestOptions result = super.transportOptions(settings);
            if (result.timeout() == null) {
                return TransportRequestOptions.builder()
                        .withCompress(result.compress())
                        .withType(result.type())
                        .withTimeout(timeout)
                        .build();
            } else {
                return result;
            }
        }

        @Override
        public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
            return action.newRequestBuilder(client);
        }

        @Override
        public Response newResponse() {
            return action.newResponse();
        }
    }
}
