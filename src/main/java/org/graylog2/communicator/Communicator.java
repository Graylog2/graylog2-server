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
package org.graylog2.communicator;

import com.beust.jcommander.internal.Lists;
import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.communicator.methods.CommunicatorMethod;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Communicator {

    private static final Logger LOG = Logger.getLogger(Communicator.class);
    
    private Core server;
    
    public Communicator(Core server) {
        this.server = server;
    }
    
    public void send(String text) {
        for (Class<? extends CommunicatorMethod> cmc : server.getCommunicatorMethods()) {
            try {
                CommunicatorMethod cm = cmc.newInstance();
                if (text.length() > cm.getMaxTextLength()) {
                    text = text.substring(0, cm.getMaxTextLength());
                }
                
                cm.send(server, text, getAllRecipientsForMethod(cmc));
            } catch (Exception e) {
                LOG.error("Could not send message from communicator. ", e);
            }
        }
    }
    
    public List<String> getAllRecipientsForMethod(Class klazz) {
        List<String> recipients = Lists.newArrayList();
        
        // XXXXXXXXXXXXXXX lolololol XXXXXXXXXXXX
        recipients.add("+491712722181");
        
        return recipients;
    }
    
}
