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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class JabberTransport implements Transport {

    private static final Logger LOG = LoggerFactory.getLogger(JabberTransport.class);
    
    public static final String NAME = "Jabber/XMPP";
    public static final String USER_FIELD_NAME = "Jabber/XMPP address";
    
    private Connection connection;
    
    private Map<String, String> configuration;
    
    @SuppressWarnings("serial")
	public static final Set<String> REQUIRED_FIELDS = new HashSet<String>() {{ 
        add("hostname");
        add("port");
        add("sasl_auth");
        add("allow_selfsigned_certs");
        add("username");
        add("password");
        add("message_prefix");
    }};
    
    @Override
    public void initialize(Map<String, String> configuration) throws TransportConfigurationException {
        // We are getting a map here for plugin compatibility. :/
        this.configuration = configuration;
        checkConfiguration();
        
        try {
            connection = connect();
        } catch(XMPPException e) {
            LOG.error("Could not connect to XMPP server.", e);
        }
    }

    @Override
    public void transportAlarm(Alarm alarm) {
        if (connection == null || !connection.isConnected()) {
            try {
                connection = connect();
            } catch(XMPPException e) {
                LOG.error("Could not connect to XMPP server.", e);
            }
        }
        
        for (AlarmReceiver receiver : alarm.getReceivers(this)) {
            try {
                LOG.debug("Sending XMPP message to alarm receiver <{}>.", receiver.getUserId());
                sendMessage(alarm, receiver);
            } catch (XMPPException e) {
                LOG.error("Could not send XMPP message to alarm receiver <" + receiver.getUserId() +">.", e);
            }
        }
    }
    
    private void sendMessage(Alarm alarm, AlarmReceiver receiver) throws XMPPException {
        LOG.debug("XMPP Receiver <{}>: [{}]", receiver.getUserId(), receiver.getAddress(this));
        ChatManager chatmanager = connection.getChatManager();
        Chat chat = chatmanager.createChat(receiver.getAddress(this), new MessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) { /* Talk to the hand. */ }
        });
        
        Message msg = new Message();
        msg.setSubject(alarm.getTopic());
        msg.setBody(configuration.get("message_prefix") + " " + alarm.getDescription());

        chat.sendMessage(msg);
    }
    
    private Connection connect() throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(
                configuration.get("hostname"),
                Integer.parseInt(configuration.get("port"))
        );
        
        config.setSASLAuthenticationEnabled(Boolean.parseBoolean(configuration.get("sasl_auth")));
        config.setSelfSignedCertificateEnabled(Boolean.parseBoolean(configuration.get("allow_selfsigned_certs")));
        Connection c = new XMPPConnection(config);
        
        c.connect();
        c.login(configuration.get("username"), configuration.get("password"));
        
        return c;
    }
    
    private void checkConfiguration() throws TransportConfigurationException {
        for (String field : REQUIRED_FIELDS) {
            if (!configSet(field)) { throw new TransportConfigurationException("Missing configuration option: " + field); }
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
    
    @Override
    public String getUserFieldName() {
        return USER_FIELD_NAME;
    }
    
    @Override
    public Map<String, String> getRequestedConfiguration() {
        // This transport is built in and has it's own config way. Just for plugin compat.
        return Maps.newHashMap();
    }
    
}
