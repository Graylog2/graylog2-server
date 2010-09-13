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

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Main;

/**
 * GELF.java: Jun 23, 2010 6:46:45 PM
 *
 * GELF utility class
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELF {

    /**
     * Is GELF enabled? Decision based on /etc/graylog2.conf "use_gelf" parameter.
     * @return boolean
     */
    public static boolean isEnabled() {
        return Main.masterConfig.getProperty("use_gelf").equals("true");
    }

}
