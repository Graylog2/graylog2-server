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
 * GELFClient.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 7:15:12 PM
 */

package org.graylog2.messagehandlers.gelf;

public class GELFClient {

    private String clientMessage = null;

    public GELFClient(String clientMessage, String threadName) {
        this.clientMessage = clientMessage;
    }

    public boolean isValidAndJSON() {
        if(!this.clientMessage.contains("{")) {
            return false;
        }
        return true;
    }

    public boolean handle() {
        // Do a quick check if this could be valid JSON.
        if (!this.isValidAndJSON()) {
            return false;
        }

        System.out.println("HANDLING CLIENT! " + this.clientMessage);

        return true;
    }

}
