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

package org.graylog2.messagehandlers.common;

import org.graylog2.Main;

/**
 * MessageCounter.java: Aug 19, 2010 6:06:20 PM
 *
 * Singleton holding the number of received messages.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MessageCounter {
    private static MessageCounter instance;

    /**
     * The API methods in this class require a hostname as String. This constant
     * defines the "all hosts/total messages" graph.
     */
    public static final String ALL_HOSTS = "all";

    private int totalCount = 0;
    private int totalSecondCount = 0;

    private int highestSecondCount = 0;

    private MessageCounter() {}

    /**
     * @return MessageCounter singleton instance
     */
    public synchronized static MessageCounter getInstance() {
        if (instance == null) {
            instance = new MessageCounter();
        }
        return instance;
    }

    /**
     * Reset count of a host
     * @param host The host to select
     */
    public void reset(String host) {
        if (host.equals(ALL_HOSTS)) {
            totalCount = 0;
        }
    }

    /**
     * Reset count of the messages per second counter.
     */
    public void resetTotalSecondCount() {
        // Possibly update highest count?
        if (totalSecondCount > highestSecondCount) {
            highestSecondCount = totalSecondCount;
        }

        totalSecondCount = 0;
    }

    /**
     * Increment count of a host (Also counts of totalSecondCounter if enabled)
     * @param host The host to select
     */
    public void countUp(String host) {
        if (host.equals(ALL_HOSTS)) {
            totalCount++;

            if (Main.printLoadStats) {
                totalSecondCount++;
            }
        }
    }

    /**
     * Get the count of a host
     * @param host The host to select
     * @return Count of the host
     */
    public int getCount(String host) {
        if (host.equals(ALL_HOSTS)) {
            return totalCount;
        }

        return 0;
    }
    
   /**
    * Get the count of the messages per second counter.
    */
    public int getTotalSecondCount() {
        return totalSecondCount;
    }

   /**
     * Get the highest recorded count of messages per second.
     */
    public int getHighestSecondCount() {
        return highestSecondCount;
    }

}