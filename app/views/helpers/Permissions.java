/*
 * Copyright 2014 TORCH UG
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
package views.helpers;

import models.User;
import models.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Permissions {

    private static final Logger log = LoggerFactory.getLogger(Permissions.class);

    public static Boolean isPermitted(String permission) {
        return isPermitted(UserService.current(), permission);
    }

    public static Boolean isPermitted(String permission, String instanceId) {
        return isPermitted(UserService.current(), permission, instanceId);
    }

    public static Boolean isPermitted(User user, String permission, String instanceId) {
        final String instancePermission = permission + ":" + instanceId;
        final boolean permitted = user.getSubject().isPermitted(instancePermission);
        if (log.isDebugEnabled()) {
            log.debug("{} has permission {}: {}", UserService.current(), instancePermission, permitted);
        }
        return permitted;
    }

    public static Boolean isPermitted(User user, String permission) {
        final boolean permitted = user.getSubject().isPermitted(permission);
        if (log.isDebugEnabled()) {
            log.debug("{} has permission {}: {}", UserService.current(), permission, permitted);
        }
        return permitted;
    }

}
