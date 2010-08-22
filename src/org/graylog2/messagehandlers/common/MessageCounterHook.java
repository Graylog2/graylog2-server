/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

/**
 * MessageCounterHook.java: Lennart Koopmann <lennart@scopeport.org> | Aug 19, 2010 5:56:21 PM
 */

package org.graylog2.messagehandlers.common;

public class MessageCounterHook implements MessagePostReceiveHookIF {
    public void process() {
        MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
    }
}