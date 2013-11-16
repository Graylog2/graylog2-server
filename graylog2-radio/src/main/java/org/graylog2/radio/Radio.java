/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.radio;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio {

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    public static final Version VERSION = RadioVersion.VERSION;

    private DateTime startedAt;
    private MetricRegistry metricRegistry;
    private Configuration configuration;

    private String nodeId;

    public void initialize(Configuration configuration, MetricRegistry metrics) {
        startedAt = new DateTime(DateTimeZone.UTC);

        NodeId id = new NodeId(configuration.getNodeIdFile());
        this.nodeId = id.readOrGenerate();

        this.metricRegistry = metrics;

        this.configuration = configuration;

        if (this.configuration.getRestTransportUri() == null) {
            String guessedIf;
            try {
                guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2-radio.conf.", e);
                throw new RuntimeException("No rest_transport_uri.");
            }

            String transportStr = "http://" + guessedIf + ":" + configuration.getRestListenUri().getPort();
            LOG.info("No rest_transport_uri set. Falling back to [{}].", transportStr);
            this.configuration.setRestTransportUri(transportStr);
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public void register() {
        LOG.info("Registering this radio instance [{}] in the Graylog2 cluster at [{}]", getNodeId(), configuration.getRestListenUri());
    }
}
