/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
package models;

import com.google.common.collect.Lists;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.RestPermissionsResponse;

import java.io.IOException;
import java.util.List;

/**
 * Combines the permissions of both the server and the web-interface.
 *
 * This does not take into account different server versions yet!
 */
public class Permissions {

    public static List<String> all() {
        List<String> permissions = Lists.newArrayList();
        try {
            RestPermissionsResponse response = ApiClient.get(RestPermissionsResponse.class).path("/system/permissions").execute();
            for (String group : response.permissions.keySet()) {
                permissions.add(group + ":*");
                for (String action : response.permissions.get(group)) {
                    permissions.add(group + ":" + action);
                }
            }
        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return permissions;
    }

}
