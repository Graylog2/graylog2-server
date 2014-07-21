/**
 * Copyright 2014 TORCH GmbH
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
package lib;

import play.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Localhost {

    public static String NAME;

    static {
        try {
            NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Logger.error("Could not get local hostname.", e);
            NAME = "unknown";
        }
    }
}
