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

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

public class JavaMailUtil {
    private JavaMailUtil() {}
    
    static Session buildSession(String protocol, boolean useAuthentication, boolean useTLS) {
        if (protocol == null || protocol.length() == 0) {
            protocol = "smtp";
        }
    
        String prefix = "mail." + protocol;
        Properties properties = new Properties(System.getProperties());
        
        properties.put("mail.transport.protocol", protocol);
        if(useAuthentication) {
            properties.put(prefix + ".auth", "true");
        }
        if("smtp".equals(protocol) && useTLS) {
            properties.put(prefix + ".starttls.enable", "true");
        }
    
        Session session = Session.getInstance(properties);
        session.setProtocolForAddress("rfc822", protocol);
        
        return session;
    }

    static Transport buildTransport(Session session, String host, int port, boolean useAuthentication, String username, String password) throws NoSuchProviderException, MessagingException {
        
        Transport transport = session.getTransport();
        transport.connect(
                host,
                port,
                useAuthentication ? username : null,
                useAuthentication ? password : null);
        
        return transport;
    }    
    
}
