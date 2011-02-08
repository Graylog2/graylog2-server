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

/**
 * MessageFilterHook.java: Feb 7, 2011 5:56:21 PM
 *
 * Filters events based on regular expression.
 *
 * @author: Joshua Spaulding <joshua.spaulding@gmail.com>
 */
public class MessageFilterHook implements MessagePostReceiveHookIF {

    /**
     * Process the hook.
     */
    public void process(Object message) {
	// 1. Get regex filters
	// 2. Match message against regular expression
	// 3. Log filter matched or dropped
    }
}
