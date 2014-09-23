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
package org.graylog2.inputs.radio;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.system.NodeId;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RadioKafkaInput extends KafkaInput {

    private static final Logger LOG = LoggerFactory.getLogger(RadioKafkaInput.class);

    public static final String NAME = "Graylog2 Radio Input (Kafka)";

    @Inject
    public RadioKafkaInput(
            final MetricRegistry metricRegistry,
            final NodeId nodeId,
            final EventBus eventBus,
            final ServerStatus serverStatus,
            final MessagePack messagePack) {
        super(metricRegistry, nodeId, eventBus, serverStatus, messagePack);
    }

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {
        configuration.setString(CK_TOPIC_FILTER, "^graylog2-radio-messages$");

        if (!checkConfig(configuration)) {
            throw new ConfigurationException(configuration.getSource().toString());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "http://graylog2.org/resources/documentation/setup/radio";
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest cr = new ConfigurationRequest();

        cr.addField(new TextField(
                CK_ZOOKEEPER,
                "ZooKeeper address",
                "192.168.1.1:2181",
                "Host and port of the ZooKeeper that is managing your Kafka cluster.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        cr.addField(new NumberField(
                        CK_FETCH_MIN_BYTES,
                        "Fetch minimum bytes",
                        5,
                        "Wait for a message batch to reach at least this size or the configured maximum wait time before fetching.",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );

        cr.addField(new NumberField(
                        CK_FETCH_WAIT_MAX,
                        "Fetch maximum wait time (ms)",
                        100,
                        "Wait for this time or the configured minimum size of a message batch before fetching.",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );

        cr.addField(new NumberField(
                        CK_THREADS,
                        "Processor threads",
                        2,
                        "Number of processor threads to spawn. Use one thread per Kafka topic partition.",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );

        return cr;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    protected boolean checkConfig(Configuration config) {
        return config.intIsSet(CK_FETCH_MIN_BYTES)
                && config.intIsSet(CK_FETCH_WAIT_MAX)
                && config.stringIsSet(CK_ZOOKEEPER)
                && config.intIsSet(CK_THREADS) && config.getInt(CK_THREADS) > 0;
    }

}