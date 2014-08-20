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
package org.graylog2.periodical;

import com.ning.http.client.AsyncHttpClient;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MasterPingPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(MasterPingPeriodical.class);

    private final ServerStatus serverStatus;
    private final Configuration configuration;
    private final AsyncHttpClient httpClient;

    @Inject
    public MasterPingPeriodical(ServerStatus serverStatus,
                                Configuration configuration,
                                AsyncHttpClient httpClient) {
        this.serverStatus = serverStatus;
        this.configuration = configuration;
        this.httpClient = httpClient;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    public void doRun() {
        try {
            Ping.ping(httpClient,
                    configuration.getGraylog2ServerUri(),
                    configuration.getRestTransportUri(),
                    serverStatus.getNodeId().toString());
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOG.error("Master ping failed.", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
