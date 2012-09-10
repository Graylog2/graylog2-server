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
package org.graylog2.communicator.methods;

import com.beust.jcommander.internal.Maps;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class TwilioCommunicator implements CommunicatorMethod {

    private static final Logger LOG = Logger.getLogger(TwilioCommunicator.class);

    public static final String PREFIX = "[gl2]";
    public static final int MAX_MESSAGE_LENGTH = 160-PREFIX.length()-1; // -1 for whitespace after prefix.
    
    GraylogServer server;
    
    @Override
    public boolean send(GraylogServer server, String text, List<String> recipients) {
        this.server = server;
        
        TwilioRestClient client = new TwilioRestClient(
                    server.getConfiguration().getTwilioSid(),
                    server.getConfiguration().getTwilioAuthToken()
        );

        return sendSms(client, "NUMBER_HERE", text);
    }
    
    private boolean sendSms(TwilioRestClient client, String receiver, String text) {
        Account acc = client.getAccount();
        
        SmsFactory smsFactory = acc.getSmsFactory();
        Map<String, String> smsParams = Maps.newHashMap();
        smsParams.put("To", receiver); // Replace with a valid phone number
        smsParams.put("From", server.getConfiguration().getTwilioSender());
        smsParams.put("Body", PREFIX + " " + text);
        
        try {
            smsFactory.create(smsParams);
        } catch (TwilioRestException e) {
            LOG.error("Could not send message to Twilio.", e);
            return false;
        }
        
        return true;
    }
    
    @Override
    public int getMaxTextLength() {
        return MAX_MESSAGE_LENGTH;
    }
    
}
