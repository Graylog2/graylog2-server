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
package org.graylog.integrations.inputs.paloalto;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.inputs.transports.SyslogTcpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.graylog.integrations.inputs.paloalto.PaloAltoCodec.CK_SYSTEM_TEMPLATE;
import static org.graylog.integrations.inputs.paloalto.PaloAltoCodec.CK_THREAT_TEMPLATE;
import static org.graylog.integrations.inputs.paloalto.PaloAltoCodec.CK_TRAFFIC_TEMPLATE;

public class PaloAltoTCPInput extends MessageInput {

    public static final String NAME = "Palo Alto Networks (TCP)";

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoTCPInput.class);

    @Inject
    public PaloAltoTCPInput(@Assisted Configuration configuration,
                            MetricRegistry metricRegistry,
                            SyslogTcpTransport.Factory transport,
                            LocalMetricRegistry localRegistry,
                            PaloAltoCodec.Factory codec,
                            Config config,
                            Descriptor descriptor,
                            ServerStatus serverStatus) {
        super(
                metricRegistry,
                configuration,
                transport.create(configuration),
                localRegistry,
                codec.create(configuration),
                config,
                descriptor,
                serverStatus);
    }

    @Override
    public void launch(InputBuffer buffer) throws MisfireException {

        // Parse the templates to log any errors immediately on input startup.
        PaloAltoTemplates templates = PaloAltoTemplates.newInstance(configuration.getString(CK_SYSTEM_TEMPLATE, PaloAltoTemplateDefaults.SYSTEM_TEMPLATE),
                                                                    configuration.getString(CK_THREAT_TEMPLATE, PaloAltoTemplateDefaults.THREAT_TEMPLATE),
                                                                    configuration.getString(CK_TRAFFIC_TEMPLATE, PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE));

        if (templates.hasErrors()) {
            throw new MisfireException(templates.errorMessageSummary("\n"));
        }

        super.launch(buffer);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<PaloAltoTCPInput> {
        @Override
        PaloAltoTCPInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {

        @Inject
        public Config(SyslogTcpTransport.Factory transport, PaloAltoCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
