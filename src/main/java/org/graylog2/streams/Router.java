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

package org.graylog2.streams;

import java.util.ArrayList;
import java.util.List;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * Router.java: Mar 16, 2011 9:40:24 PM
 *
 * Routes a GELF Message to it's streams.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Router {

    // Hidden.
    private Router() { }

    public static List<Integer> route(GELFMessage msg) {
        ArrayList<Integer> streams = new ArrayList<Integer>();

        streams.add(1);
        streams.add(2);
        streams.add(10);

        return streams;
    }

}