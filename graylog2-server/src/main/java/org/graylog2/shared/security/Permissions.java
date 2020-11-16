/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.security;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class Permissions {
    private static final Logger LOG = LoggerFactory.getLogger(Permissions.class);

    private final Set<String> allPermissions;
    private final Set<String> readerBasePermissions;
    private final Map<String, Collection<String>> allPermissionsMap;

    @Inject
    public Permissions(final Set<PluginPermissions> pluginPermissions) {
        this.allPermissions = buildPermissions(pluginPermissions, PluginPermissions::permissions);
        this.readerBasePermissions = buildPermissions(pluginPermissions, PluginPermissions::readerBasePermissions);
        this.allPermissionsMap = buildPermissionsMap(allPermissions);
    }

    public Set<String> allPermissions() {
        return allPermissions;
    }

    public Map<String, Collection<String>> allPermissionsMap() {
        return allPermissionsMap;
    }

    public Set<String> readerBasePermissions() {
        return readerBasePermissions;
    }

    public Set<String> readerPermissions(String username) {
        final ImmutableSet.Builder<String> perms = ImmutableSet.<String>builder().addAll(readerBasePermissions);

        if (isNullOrEmpty(username)) {
            LOG.error("Username cannot be empty or null for creating reader permissions");
            throw new IllegalArgumentException("Username was null or empty when getting reader permissions.");
        }

        perms.addAll(userSelfEditPermissions(username));

        return perms.build();
    }

    public Set<String> userSelfEditPermissions(String username) {
        ImmutableSet.Builder<String> perms = ImmutableSet.builder();
        perms.add(perInstance(RestPermissions.USERS_EDIT, username));
        perms.add(perInstance(RestPermissions.USERS_PASSWORDCHANGE, username));
        perms.add(perInstance(RestPermissions.USERS_TOKENLIST, username));
        perms.add(perInstance(RestPermissions.USERS_TOKENCREATE, username));
        perms.add(perInstance(RestPermissions.USERS_TOKENREMOVE, username));
        return perms.build();
    }

    private String perInstance(String permission, String instance) {
        // TODO check for existing instance etc (use DomainPermission subclass)
        return permission + ":" + instance;
    }

    private interface PermissionListCallback {
        Set<Permission> permissions(PluginPermissions permissions);
    }

    private static Set<String> buildPermissions(Set<PluginPermissions> pluginPermissions, PermissionListCallback callback) {
        final Set<String> permissionSet = new HashSet<>();

        for (PluginPermissions pluginPermission : pluginPermissions) {
            for (Permission permission : callback.permissions(pluginPermission)) {
                if (!permissionSet.contains(permission.permission())) {
                    permissionSet.add(permission.permission());
                } else {
                    LOG.error("Error adding permissions for plugin: {}", pluginPermission.getClass().getCanonicalName());
                    throw new IllegalArgumentException("Duplicate permission found. Permission \"" + permission.toString() + "\" already exists!");
                }
            }
        }

        return ImmutableSet.copyOf(permissionSet);
    }

    private Map<String, Collection<String>> buildPermissionsMap(Set<String> permissions) {
        final ListMultimap<String, String> all = ArrayListMultimap.create();

        for (String permission : permissions) {
            final Iterator<String> split = Splitter.on(':').limit(2).split(permission).iterator();
            final String group = split.next();
            final String action = split.next();

            all.put(group, action);
        }

        // Create an immutable copy of the map and the collections inside it.
        return ImmutableMap.copyOf(all.asMap().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ImmutableSet.copyOf(e.getValue()))));
    }
}
