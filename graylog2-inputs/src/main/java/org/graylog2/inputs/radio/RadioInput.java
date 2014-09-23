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
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.system.NodeId;
import org.msgpack.MessagePack;

import javax.inject.Inject;

public class RadioInput extends RadioKafkaInput {

    /*
     * RadioInput became RadioKafkaInput when RadioAMQPInput was introduced. This
     * is just a class for convenience of all the users that had already launched
     * a RadioInput. Inputs are launched by class name for plugin flexibility.
     *
     * lol naming.
     */

    public static final String NAME = "Graylog2 legacy Radio Input (Kafka)";

    @Override
    public String getName() {
        return NAME;
    }

    @Inject
    public RadioInput(
            final MetricRegistry metricRegistry,
            final NodeId nodeId,
            final EventBus eventBus,
            final ServerStatus serverStatus,
            final MessagePack messagePack) {
        super(metricRegistry, nodeId, eventBus, serverStatus, messagePack);
    }

}
