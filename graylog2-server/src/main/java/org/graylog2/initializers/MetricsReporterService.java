/**
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
package org.graylog2.initializers;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.metrics.MongoDbMetricsReporter;
import org.graylog2.plugin.ServerStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class MetricsReporterService extends AbstractIdleService {
    private final Configuration configuration;
    private final MetricRegistry metricRegistry;
    private final MongoConnection mongoConnection;
    private final ServerStatus serverStatus;
    private MongoDbMetricsReporter metricsReporter = null;

    @Inject
    public MetricsReporterService(Configuration configuration,
                                  MetricRegistry metricRegistry,
                                  MongoConnection mongoConnection,
                                  ServerStatus serverStatus) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.mongoConnection = mongoConnection;
        this.serverStatus = serverStatus;
    }

    @Override
    protected void startUp() throws Exception {
        if (!configuration.isMetricsCollectionEnabled())
            return;
        metricsReporter = MongoDbMetricsReporter.forRegistry(metricRegistry, mongoConnection, serverStatus).build();
        metricsReporter.start(1, TimeUnit.SECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        if (!configuration.isMetricsCollectionEnabled())
            return;
        if (metricsReporter != null)
            metricsReporter.stop();
    }
}
