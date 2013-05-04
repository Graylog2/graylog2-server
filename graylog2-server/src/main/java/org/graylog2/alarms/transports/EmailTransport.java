/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.alarms.transports;

import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.common.collect.Maps;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class EmailTransport implements Transport {

    private static final Logger LOG = LoggerFactory.getLogger(EmailTransport.class);
    
    private static final String NAME = "Email";
    private static final String USER_FIELD_NAME = "Email address";

    public static final Set<String> REQUIRED_FIELDS = new HashSet<String>(Arrays.asList( 
        "subject_prefix",
        "hostname",
        "port",
        "use_tls",
        "use_auth",
        "from_email",
        "from_name"));

    private Map<String, String> pluginConfiguration;
    private Session session;
    private InternetAddress from;
    private EmailLayout layout = new HtmlEmailLayout();


    @Override
    public void initialize(Map<String, String> pluginConfiguration) throws TransportConfigurationException {
        // We are getting a map here for plugin compatibility. :/
        this.pluginConfiguration = pluginConfiguration;
        checkConfiguration();
        
        this.session = JavaMailUtil.buildSession(
                pluginConfiguration.get("protocol"),
                Boolean.parseBoolean(pluginConfiguration.get("use_auth")),
                Boolean.parseBoolean(pluginConfiguration.get("use_tls")));
        
        this.from = toAddress(pluginConfiguration.get("from_email"), pluginConfiguration.get("from_name"));
        
        this.layout.initialize(pluginConfiguration);
    }
    
    @Override
    public void transportAlarm(Alarm alarm) {
        long utcTimestamp = Tools.getUTCTimestamp();
        javax.mail.Transport transport = null;
        try {
            for (AlarmReceiver receiver : alarm.getReceivers(this)) {
                transport = send(transport, alarm, receiver, utcTimestamp);
            }
        } catch(Exception e) {
            LOG.warn("Could not send alarm email.", e);
        } finally {
            if(null != transport) {
                try {
                    transport.close();
                } catch(MessagingException ignore) {}
            }
        }
    }
    
    private javax.mail.Transport send(javax.mail.Transport transport, Alarm alarm, AlarmReceiver receiver, long utcTimestamp) throws Exception {
        if(null == transport) {
            transport = JavaMailUtil.buildTransport(
                    session,
                    pluginConfiguration.get("hostname"),
                    Integer.parseInt(pluginConfiguration.get("port")),
                    Boolean.parseBoolean(pluginConfiguration.get("use_auth")),
                    pluginConfiguration.get("username"),
                    pluginConfiguration.get("password"));
        }

        MimeUtil.sendMessage(
                session,
                transport,
                from,
                toAddress(receiver.getAddress(this)),
                layout.getSubject(alarm, utcTimestamp),
                layout.formatMessageBody(alarm, utcTimestamp),
                layout.getContentType());
        return transport;
    }

    private void checkConfiguration() throws TransportConfigurationException {
        for (String field : REQUIRED_FIELDS) {
            if (!configSet(field)) { throw new TransportConfigurationException("Missing configuration option: " + field); }
        }
        
        if (pluginConfiguration.get("use_auth").equals("true")) {
            if (!configSet("username")) { throw new TransportConfigurationException("Missing configuration option: username"); }
            if (!configSet("password")) { throw new TransportConfigurationException("Missing configuration option: password"); }
        }
    }
    
    private boolean configSet(String key) {
        return pluginConfiguration != null && pluginConfiguration.containsKey(key)
                && pluginConfiguration.get(key) != null && !pluginConfiguration.get(key).isEmpty();
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getUserFieldName() {
        return USER_FIELD_NAME;
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // This transport is built in and has it's own config way. Just for plugin compat.
        return Maps.newHashMap();
    }
    
    private InternetAddress toAddress(String email, String name) throws TransportConfigurationException {
        try {
            InternetAddress address = toAddress(email);
            address.setPersonal(name);
            return address;
        } catch (UnsupportedEncodingException e) {
            throw new TransportConfigurationException("Could not encode name: " + name + "; " + e.getMessage());
        }
    }
    
    private InternetAddress toAddress(String email) throws TransportConfigurationException {
        try {
            return new InternetAddress(email);
        } catch(AddressException e) {
            throw new TransportConfigurationException("Could not parse email address: " + email + "; " + e.getMessage());
        }
    }
}
