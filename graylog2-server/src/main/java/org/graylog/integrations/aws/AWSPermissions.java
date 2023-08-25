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
package org.graylog.integrations.aws;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.security.Permission.create;

public class AWSPermissions implements PluginPermissions {

    public static final String AWS_READ = "aws:read";

    private final ImmutableSet<Permission> permissions = ImmutableSet.of(
            create(AWS_READ, "Read access for AWS")
    );

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }

    public Set<String> allPermissions() {
        return new AWSPermissions().permissions().stream()
                                   .map(Permission::permission)
                                   .collect(Collectors.toSet());
    }
}