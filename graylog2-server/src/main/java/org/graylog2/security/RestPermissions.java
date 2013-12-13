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
package org.graylog2.security;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class RestPermissions {
    // These should all be in the form of "group:action", because allPermissions() below depends on it.
    // Should this ever change, you need to adapt the code below, too.
    public static final String USERS_CREATE =  "users:create";
    public static final String USERS_EDIT = "users:edit";
    public static final String USERS_LIST = "users:list";
    public static final String USERS_PERMISSIONSEDIT = "users:permissionsedit";
    public static final String USERS_PASSWORDCHANGE = "users:passwordchange";
    public static final String USERS_TOKENCREATE = "users:tokencreate";
    public static final String USERS_TOKENLIST = "users:tokenlist";
    public static final String USERS_TOKENREMOVE = "users:tokenremove";

    private static Map<String, Collection<String>> allPermissions;

    public static synchronized Map<String, Collection<String>> allPermissions() {
        if (allPermissions == null) {
            final Field[] declaredFields = RestPermissions.class.getDeclaredFields();
            ListMultimap<String, String> all = ArrayListMultimap.create();
            for (Field declaredField : declaredFields) {
                if (! Modifier.isStatic(declaredField.getModifiers())) {
                    continue;
                }
                if (! String.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                declaredField.setAccessible(true);
                try {
                    final String permission = (String) declaredField.get(RestPermissions.class);
                    final Iterator<String> split = Splitter.on(':').limit(2).split(permission).iterator();
                    final String group = split.next();
                    final String action = split.next();
                    all.put(group, action);
                } catch (IllegalAccessException ignored) {
                }
            }
            allPermissions = all.asMap();
        }
        return allPermissions;
    }

}
