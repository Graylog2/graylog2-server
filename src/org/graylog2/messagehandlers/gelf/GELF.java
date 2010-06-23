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
 * GELFThread.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 6:46:45 PM
 */

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Main;

public class GELF {

    public static final int CLIENT_TIMEOUT = 5000;

    public static boolean isEnabled() {
        if (Main.masterConfig.getProperty("use_gelf").equals("true")) {
            return true;
        }

        return false;
    }

}
