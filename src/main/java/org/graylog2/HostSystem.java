/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import org.graylog2.database.MongoBridge;

/**
 * HostSystem.java: Jan 16, 2011 2:11:09 PM
 *
 * Utility class that provides access to host system information.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostSystem  {

    /**
     * @return total number of processors or cores available to the JVM
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * @return Total amount of memory currently in use by the JVM (bytes)
     */
    public static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * @return Maximum amount of memory the JVM will attempt to use (bytes). (0 if unlimited)
     */
    public static long getMaxMemory() {
        long max = Runtime.getRuntime().maxMemory();
        return max == Long.MAX_VALUE ? 0 : max;
    }

    /**
     * @return Total amount of free memory available to the JVM (bytes)
     */
    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static void writeSystemHealthHistorically() {
        MongoBridge m = new MongoBridge();
        m.writeHistoricServerValue("used_memory", getUsedMemory());
        m.writeHistoricServerValue("max_memory", getMaxMemory());
    }

}