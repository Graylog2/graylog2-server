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
package org.graylog2.activities;

import org.graylog2.communicator.Communicator;
import org.graylog2.database.MongoBridge;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ActivityWriter {
    
    MongoBridge mongoBridge;
    Communicator communicator;
    
    public ActivityWriter(MongoBridge mongoBridge, Communicator communicator) {
        this.mongoBridge = mongoBridge;
        this.communicator = communicator;
    }
    
    public void write(Activity activity) {
        mongoBridge.writeActivity(activity);
        communicator.send(activity.getContent());
    }
    
}
