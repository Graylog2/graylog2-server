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
package org.graylog2.initializers;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.initializers.AbstractJerseyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;

public class WebInterfaceService extends AbstractJerseyService {
    private static final Logger LOG = LoggerFactory.getLogger(WebInterfaceService.class);

    private final BaseConfiguration configuration;

    @Inject
    public WebInterfaceService(ObjectMapper objectMapper,
                               BaseConfiguration configuration,
                               MetricRegistry metricRegistry) {
        super(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), objectMapper, metricRegistry);
        this.configuration = configuration;
    }

    @Override
    protected void startUp() throws Exception {
        final String[] resources = new String[]{"org.graylog2.web.resources"};

        httpServer = setUp("web",
                configuration.getWebListenUri(),
                configuration.isWebEnableTls(),
                configuration.getWebTlsCertFile(),
                configuration.getWebTlsKeyFile(),
                configuration.getWebTlsKeyPassword(),
                configuration.getWebThreadPoolSize(),
                configuration.getWebMaxInitialLineLength(),
                configuration.getWebMaxHeaderSize(),
                configuration.isWebEnableGzip(),
                configuration.isWebEnableCors(),
                Collections.emptySet(),
                resources);

        httpServer.start();

        LOG.info("Started Web Interface at <{}>", configuration.getWebListenUri());
    }

    @Override
    protected void shutDown() throws Exception {
        if (httpServer != null && httpServer.isStarted()) {
            LOG.info("Shutting down Web Interface at <{}>", configuration.getWebListenUri());
            httpServer.shutdownNow();
        }
    }
}
