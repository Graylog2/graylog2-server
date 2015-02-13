/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package models;

import com.google.common.collect.Lists;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.User;
import play.Play;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;


public class LocalAdminUser extends User {

    private static AtomicReference<models.LocalAdminUser> instance = new AtomicReference<>(null);

    LocalAdminUser(ApiClient api, String id, String name, String email, String fullName, List<String> permissions, String passwordHash, String tz) {
        super(api, id, name, email, fullName, permissions, passwordHash, tz, true, false, null, 0, Collections.<String, Object>emptyMap());
    }

    public static void createSharedInstance(ApiClient api, String username, String passwordHash) {
        final models.LocalAdminUser adminUser = new models.LocalAdminUser(api,"0", username, "None",  "Interface Admin", Lists.newArrayList("*"), passwordHash, null);
        if (! instance.compareAndSet(null, adminUser)) {
            // unless we are in test mode, this would be a bug.
            if (! Play.application().isTest()) {
                throw new IllegalStateException("Attempted to reset the local admin user object. This is a bug.");
            }
        }
    }

    public static models.LocalAdminUser getInstance() {
        return instance.get();
    }

}
