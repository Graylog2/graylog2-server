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
package org.graylog2.inputs.transports;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;

public class RadioKafkaTransport extends KafkaTransport {

    @AssistedInject
    public RadioKafkaTransport(@Assisted Configuration configuration,
                               LocalMetricRegistry localRegistry,
                               NodeId nodeId,
                               EventBus serverEventBus,
                               ServerStatus serverStatus,
                               @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        super(setDefaultConfig(configuration), localRegistry, nodeId, serverEventBus, serverStatus, scheduler);
    }

    private static Configuration setDefaultConfig(Configuration configuration) {
        configuration.setString(CK_TOPIC_FILTER, "^graylog2-radio-messages$");
        return configuration;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<RadioKafkaTransport> {
        @Override
        RadioKafkaTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends KafkaTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            // we provide a default value for that setting
            r.removeField(CK_TOPIC_FILTER);
            return r;
        }
    }
}
