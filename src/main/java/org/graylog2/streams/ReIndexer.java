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

/**
 * ReIndexer.java: Mar 16, 2011 10:07:21 PM
 *
 * Cycles over the whole messages collection and re-calculates
 * the stream information.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ReIndexer {

    // Hidden.
    private ReIndexer() { }

    public static final int ALL_STREAMS = 0;

    public static boolean run(int stream_id) {
        // Cycle trough every message.
        return true;
    }

}