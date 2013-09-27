/*
 * Copyright 2013 TORCH UG
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
 */
package lib;

import com.google.common.collect.Lists;
import play.Logger;
import play.Play;

import java.util.List;

public class Configuration {

    // Variables that can be overriden. (for example in tests)
    private static String graylog2ServerUris = Play.application().configuration().getString("graylog2-server.uris");
    private static String userName = Play.application().configuration().getString("local-user.name");
    private static String passwordHash = Play.application().configuration().getString("local-user.password-sha1");


	public static List<String> getServerRestUris() {
        List<String> uris = Lists.newArrayList();

        // TODO make this more robust and fault tolerant. just a quick hack to get it working for now.
        for (String uri : graylog2ServerUris.split(",")) {
            if (uri != null && !uri.endsWith("/")) {
                uri += "/";
            }

            uris.add(uri);
        }
		
		return uris;
	}

    public static void setServerRestUris(String URIs) {
        graylog2ServerUris = URIs;
        Logger.info("graylog2-server.uris overridden with <" + URIs + ">.");
    }

    public static void setUserName(String username) {
        Logger.info("local-user.name overridden with <" + username + ">.");
        userName = username;
    }

    public static void setPassword(String password) {
        Logger.info("local-user.password-sha1 overridden with <" + passwordHash + ">.");
        passwordHash = password;
    }
	
}
