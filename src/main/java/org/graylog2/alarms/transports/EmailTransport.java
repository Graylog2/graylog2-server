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

import java.util.Map;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;

import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.graylog2.plugin.alarms.AlarmReceiver;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class EmailTransport implements Transport {

    private static final Logger LOG = Logger.getLogger(EmailTransport.class);
    
    private static final String NAME = "Email";
    
    private final String className;
    
    private Map<String, String> configuration;
    
    public EmailTransport() {
        this.className = this.getClass().getCanonicalName();
    }

    @Override
    public void initialize(Map<String, String> configuration) throws TransportConfigurationException {
        // We are getting a map here for plugin compatibility. :/
        this.configuration = configuration;
        checkConfiguration();
    }
    
    @Override
    public void transportAlarm(Alarm alarm) {
        try {
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
            
            for (AlarmReceiver receiver : alarm.getReceivers(this)) {
                email.addTo(receiver.getAddress(this));
            }
            
            String subjectPrefix = configuration.get("subject_prefix");
            String subject = alarm.getTopic();
            
            if (subjectPrefix != null && !subjectPrefix.isEmpty()) {
                subject = subjectPrefix + " " + subject;
            }
            
            email.setSubject(subject);
            email.setMsg(alarm.getDescription());
            email.send();
        } catch(Exception e) {
            LOG.warn("Could not send alarm email.", e);
        }
    }

    private void checkConfiguration() throws TransportConfigurationException {
        if (!configSet("subject_prefix")) { throw new TransportConfigurationException("Missing configuration option: subject_prefix"); }
        if (!configSet("hostname")) { throw new TransportConfigurationException("Missing configuration option: hostname"); }
        if (!configSet("port")) { throw new TransportConfigurationException("Missing configuration option: port"); }
        if (!configSet("use_tls")) { throw new TransportConfigurationException("Missing configuration option: use_tls"); }
        if (!configSet("from_email")) { throw new TransportConfigurationException("Missing configuration option: from_email"); }
        if (!configSet("from_name")) { throw new TransportConfigurationException("Missing configuration option: from_name"); }
        
        if (configSet("use_auth")) {
            if (configuration.get("use_auth").equals("true")) {
                if (!configSet("username")) { throw new TransportConfigurationException("Missing configuration option: username"); }
                if (!configSet("password")) { throw new TransportConfigurationException("Missing configuration option: password"); }
            }
        } else {
            throw new TransportConfigurationException("Missing configuration option: hostname");
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
