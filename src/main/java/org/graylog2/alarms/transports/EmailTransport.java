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

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class EmailTransport implements Transport {

    private static final Logger LOG = LoggerFactory.getLogger(EmailTransport.class);
    
    private static final String NAME = "Email";

    private Map<String, String> configuration;
    public static final Set<String> REQUIRED_FIELDS = new HashSet<String>() {{ 
        add("subject_prefix");
        add("hostname");
        add("port");
        add("use_tls");
        add("use_auth");
        add("from_email");
        add("from_name");
    }};

    @Override
    public void initialize(Map<String, String> configuration) throws TransportConfigurationException {
        // We are getting a map here for plugin compatibility. :/
        this.configuration = configuration;
        checkConfiguration();
    }
    
    @Override
    public void transportAlarm(Alarm alarm) {
        try {
            for (AlarmReceiver receiver : alarm.getReceivers(this)) {
                send(alarm, receiver);
            }
        } catch(Exception e) {
            LOG.warn("Could not send alarm email.", e);
        }
    }
    
    private void send(Alarm alarm, AlarmReceiver receiver) throws EmailException {
        SimpleEmail email = new SimpleEmail();

        email.setHostName(configuration.get("hostname"));
        email.setSmtpPort(Integer.parseInt(configuration.get("port")));

        if (configuration.get("use_auth").equals("true")) {
            email.setAuthentication(configuration.get("username"), configuration.get("password"));
            if (configuration.get("use_tls").equals("true")) {
                email.setTLS(true);
            }
        }

        email.setFrom(configuration.get("from_email"), configuration.get("from_name"));

        
        email.addTo(receiver.getAddress(this));

        String subjectPrefix = configuration.get("subject_prefix");
        String subject = alarm.getTopic();

        if (subjectPrefix != null && !subjectPrefix.isEmpty()) {
            subject = subjectPrefix + " " + subject;
        }

        email.setSubject(subject);
        email.setMsg(alarm.getDescription());
        email.send();
    }

    
    private void checkConfiguration() throws TransportConfigurationException {
        for (String field : REQUIRED_FIELDS) {
            if (!configSet(field)) { throw new TransportConfigurationException("Missing configuration option: " + field); }
        }
        
        if (configuration.get("use_auth").equals("true")) {
            if (!configSet("username")) { throw new TransportConfigurationException("Missing configuration option: username"); }
            if (!configSet("password")) { throw new TransportConfigurationException("Missing configuration option: password"); }
        }
    }
    
    private boolean configSet(String key) {
        return configuration != null && configuration.containsKey(key)
                && configuration.get(key) != null && !configuration.get(key).isEmpty();
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
}
