/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.periodical;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.graylog2.Log;
import org.graylog2.messagehandlers.gelf.ChunkedGELFClientManager;
import org.graylog2.messagehandlers.gelf.ChunkedGELFMessage;

/**
 * ChunkedGELFClientManagerThread.java: Sep 20, 2010 9:28:37 PM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFClientManagerThread extends Thread {

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            try {
                ////////// DEBUG ONLY
                HashMap<String, ChunkedGELFMessage> messageMap = ChunkedGELFClientManager.getInstance().getMessageMap();
                Set set = messageMap.keySet();
                Iterator iter = set.iterator();
                int i = 0;
                while(iter.hasNext()) {
                    String messageId = (String) iter.next();
                    System.out.println("MESSAGE " + i + ": " + messageId);
                    i++;
                }
                ////////////////////

                ////////Set invalidMesssage = ChunkedGELFClientManager.getInstance().getInvalidMessages();
                
            } catch (Exception e) {
                Log.warn("Error in ChunkedGELFClientManagerThread: " + e.toString());
            }

           // Run every 10 seconds.
           try { Thread.sleep(10000); } catch(InterruptedException e) {}
        }
    }

}