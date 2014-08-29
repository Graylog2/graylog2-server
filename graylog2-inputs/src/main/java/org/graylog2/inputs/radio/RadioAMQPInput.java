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
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RadioAMQPInput extends AMQPInput {

    public static final String NAME = "Graylog2 Radio Input (AMQP)";

    @Inject
    public RadioAMQPInput(MetricRegistry metricRegistry, EventBus eventBus) {
        super(metricRegistry, eventBus);
    }

    @Override
    protected String defaultRouttingKey() {
        return "graylog2-radio-message";
    }

    @Override
    protected String defaultExchangeName() {
        return "graylog2";
    }

    @Override
    protected String defaultQueueName() {
        return "graylog2-radio-messages";
    }

    @Override
    public String getName() {
        return NAME;
    }

}
