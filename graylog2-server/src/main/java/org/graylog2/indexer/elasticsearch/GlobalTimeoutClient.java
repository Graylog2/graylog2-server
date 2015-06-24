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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.unit.TimeValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class GlobalTimeoutClient extends FilterClient {
    private final long timeout;
    private final TimeUnit unit;

    public GlobalTimeoutClient(Client in, long timeout, TimeUnit unit) {
        super(in);

        checkArgument(timeout > 0);
        this.timeout = timeout;
        this.unit = checkNotNull(unit);
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, Client>> ActionFuture<Response> execute(Action<Request, Response, RequestBuilder, Client> action, Request request) {
        return GlobalTimeoutActionFuture.create(super.execute(action, request), timeout, unit);
    }

    @Override
    public ClusterAdminClient cluster() {
        return new GlobalTimeoutClusterAdminClient(super.cluster(), timeout, unit);
    }

    @Override
    public IndicesAdminClient indices() {
        return new GlobalTimeoutIndicesAdminClient(super.indices(), timeout, unit);
    }

    @Override
    public AdminClient admin() {
        return new GlobalTimeoutAdminClient(super.admin(), timeout, unit);
    }

    public static class GlobalTimeoutAdminClient implements AdminClient {
        private final AdminClient in;
        private final long timeout;
        private final TimeUnit unit;

        public GlobalTimeoutAdminClient(AdminClient in, long timeout, TimeUnit unit) {
            this.in = checkNotNull(in);

            checkArgument(timeout > 0);
            this.timeout = timeout;
            this.unit = checkNotNull(unit);
        }

        @Override
        public IndicesAdminClient indices() {
            return new GlobalTimeoutIndicesAdminClient(in.indices(), timeout, unit);
        }

        @Override
        public ClusterAdminClient cluster() {
            return new GlobalTimeoutClusterAdminClient(in.cluster(), timeout, unit);
        }
    }

    public static class GlobalTimeoutClusterAdminClient extends FilterClient.ClusterAdmin {
        private final long timeout;
        private final TimeUnit unit;

        public GlobalTimeoutClusterAdminClient(ClusterAdminClient in, long timeout, TimeUnit unit) {
            super(in);

            checkArgument(timeout > 0);
            this.timeout = timeout;
            this.unit = checkNotNull(unit);
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, ClusterAdminClient>> ActionFuture<Response> execute(Action<Request, Response, RequestBuilder, ClusterAdminClient> action, Request request) {
            return GlobalTimeoutActionFuture.create(super.execute(action, request), timeout, unit);
        }
    }

    public static class GlobalTimeoutIndicesAdminClient extends FilterClient.IndicesAdmin {
        private final long timeout;
        private final TimeUnit unit;

        public GlobalTimeoutIndicesAdminClient(IndicesAdminClient in, long timeout, TimeUnit unit) {
            super(in);

            checkArgument(timeout > 0);
            this.timeout = timeout;
            this.unit = checkNotNull(unit);
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder, IndicesAdminClient>> ActionFuture<Response> execute(Action<Request, Response, RequestBuilder, IndicesAdminClient> action, Request request) {
            return GlobalTimeoutActionFuture.create(super.execute(action, request), timeout, unit);
        }
    }

    public static class GlobalTimeoutActionFuture<T> implements ActionFuture<T> {
        private final ActionFuture<T> in;
        private final long timeout;
        private final TimeUnit unit;

        private GlobalTimeoutActionFuture(ActionFuture<T> in, long timeout, TimeUnit unit) {
            this.in = checkNotNull(in);

            checkArgument(timeout > 0);
            this.timeout = timeout;
            this.unit = checkNotNull(unit);
        }

        public static <Response extends ActionResponse> GlobalTimeoutActionFuture<Response> create(ActionFuture<Response> actionFuture, long timeout, TimeUnit unit) {
            return actionFuture == null ? null : new GlobalTimeoutActionFuture<>(actionFuture, timeout, unit);
        }

        @Override
        public T actionGet() throws ElasticsearchException {
            return in.actionGet(timeout, unit);
        }

        @Override
        public T actionGet(String timeout) throws ElasticsearchException {
            return in.actionGet(timeout);
        }

        @Override
        public T actionGet(long timeoutMillis) throws ElasticsearchException {
            return in.actionGet(timeoutMillis);
        }

        @Override
        public T actionGet(long timeout, TimeUnit unit) throws ElasticsearchException {
            return in.actionGet(timeout, unit);
        }

        @Override
        public T actionGet(TimeValue timeout) throws ElasticsearchException {
            return in.actionGet(timeout);
        }

        @Override
        public Throwable getRootFailure() {
            return in.getRootFailure();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return in.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return in.isCancelled();
        }

        @Override
        public boolean isDone() {
            return in.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return in.get(timeout, unit);
            } catch (TimeoutException e) {
                throw new ExecutionException(e.getCause());
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return in.get(timeout, unit);
        }
    }
}
