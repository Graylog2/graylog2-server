/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.radio;

import org.graylog2.inputs.amqp.AMQPInput;
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

    @Override
    public void checkConfiguration() throws ConfigurationException {
        configuration.setString(CK_EXCHANGE, "graylog2");
        configuration.setString(CK_QUEUE, "graylog2-radio-messages");
        configuration.setString(CK_ROUTING_KEY, "graylog2-radio-message");

        if (!checkConfig(configuration)) {
            throw new ConfigurationException(configuration.getSource().toString());
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest cr = new ConfigurationRequest();

        cr.addField(
                new TextField(
                        CK_HOSTNAME,
                        "Broker hostname",
                        "",
                        "Hostname of the AMQP broker to use",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        cr.addField(
                new NumberField(
                        CK_PORT,
                        "Broker port",
                        5672,
                        "Port of the AMQP broker to use",
                        ConfigurationField.Optional.OPTIONAL,
                        NumberField.Attribute.IS_PORT_NUMBER
                )
        );

        cr.addField(
                new TextField(
                        CK_VHOST,
                        "Broker virtual host",
                        "/",
                        "Virtual host of the AMQP broker to use",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        cr.addField(
                new TextField(
                        CK_USERNAME,
                        "Username",
                        "",
                        "Username to connect to AMQP broker",
                        ConfigurationField.Optional.OPTIONAL
                )
        );

        cr.addField(
                new TextField(
                        CK_PASSWORD,
                        "Password",
                        "",
                        "Password to connect to AMQP broker",
                        ConfigurationField.Optional.OPTIONAL,
                        TextField.Attribute.IS_PASSWORD
                )
        );

        cr.addField(
                new NumberField(
                        CK_PREFETCH,
                        "Prefetch count",
                        0,
                        "For advanced usage: AMQP prefetch count. Default is 0 (unlimited).",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        return cr;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isExclusive() {
        return true;
    }


}
