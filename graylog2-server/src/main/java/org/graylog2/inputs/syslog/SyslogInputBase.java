/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.inputs.syslog;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SyslogInputBase {

    public static final String CK_BIND_ADDRESS = "bind_address";
    public static final String CK_PORT = "port";
    public static final String CK_FORCE_RDNS = "force_rdns";
    public static final String CK_ALLOW_OVERRIDE_DATE = "allow_override_date";

    protected boolean checkConfig(Configuration config) {
        return config.stringIsSet(CK_BIND_ADDRESS)
                && config.intIsSet(CK_PORT)
                && config.boolIsSet(CK_FORCE_RDNS)
                && config.boolIsSet(CK_ALLOW_OVERRIDE_DATE);
    }

    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r = new ConfigurationRequest();

        r.addField(
                new TextField(
                        CK_BIND_ADDRESS,
                        "0.0.0.0",
                        "Address to listen on. For example 0.0.0.0 or 127.0.0.1.",
                        TextField.Attribute.IS_SOCKET_ADDRESS
                )
        );

        r.addField(
                new NumberField(
                        CK_PORT,
                        514,
                        "Port to listen on.",
                        NumberField.Attribute.IS_PORT_NUMBER
                )
        );

        r.addField(
                new BooleanField(
                        CK_FORCE_RDNS,
                        false,
                        "Force rDNS resolution of hostname? Use if hostname cannot be parsed."
                )
        );

        r.addField(
                new BooleanField(
                        CK_ALLOW_OVERRIDE_DATE,
                        true,
                        "Allow to override with current date if date could not be parsed."
                )
        );

        return r;
    }

}
