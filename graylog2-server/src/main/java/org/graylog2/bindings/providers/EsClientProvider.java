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
package org.graylog2.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.graylog2.indexer.elasticsearch.GlobalTimeoutClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class EsClientProvider implements Provider<Client> {
    private final Node node;
    private final MetricRegistry metricRegistry;
    private final Duration requestTimeout;

    @Inject
    public EsClientProvider(Node node,
                            MetricRegistry metricRegistry,
                            @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.node = node;
        this.metricRegistry = metricRegistry;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Client get() {
        return new GlobalTimeoutClient(node.client(),
                                       requestTimeout.getQuantity(),
                                       requestTimeout.getUnit(),
                                       metricRegistry);
    }
}
