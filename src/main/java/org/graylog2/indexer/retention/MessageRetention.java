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

package org.graylog2.indexer.retention;

import org.apache.log4j.Logger;
import org.graylog2.ServerValue;
import org.graylog2.Tools;

/**
 * MessageRetention.java: Nov 22, 2011 6:58:31 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageRetention {

    private static final Logger LOG = Logger.getLogger(MessageRetention.class);

    public static void performCleanup(int timeDays) {
    }

    public static void updateLastPerformedTime() {
        ServerValue.writeMessageRetentionLastPerformed(Tools.getUTCTimestamp());
    }

}
