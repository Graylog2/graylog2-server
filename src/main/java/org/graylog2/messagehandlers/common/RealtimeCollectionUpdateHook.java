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

package org.graylog2.messagehandlers.common;

import org.graylog2.Main;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * RealtimeCollectionUpdateHook.java: Jan 01, 2012 1:25:36 PM
 *
 * Updates the realtime message collection in MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class RealtimeCollectionUpdateHook implements MessagePostReceiveHookIF {

    @Override
    public void process(GELFMessage message) {
        if (!Main.configuration.enableRealtimeCollection()) {
            return;
        }

        MongoBridge m = new MongoBridge();
        m.writeToRealtimeCollection(message);
    }

}