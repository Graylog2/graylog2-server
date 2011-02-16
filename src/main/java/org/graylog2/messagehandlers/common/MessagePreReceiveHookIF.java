/**
 * Copyright 2010 Lennart Koopmann <llennart@socketfeed.com>
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

/**
 * MessagePostReceiveHookIF.java: Aug 19, 2010 5:46:32 PM
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessagePreReceiveHookIF {

    /**
     * Process the hook.
     */
    public void process(Object payload);

}