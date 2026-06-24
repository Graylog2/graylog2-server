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
package org.graylog.events.search;

import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog2.shared.security.RestPermissions;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controls event definition filtering for event searches based on user permissions.
 * <p>
 * Use {@link #allAllowed()} when the user has blanket {@code eventdefinitions:read} permission
 * or when event definition permission enforcement is disabled globally.
 * Use {@link #allowList(Set)} to restrict results to events whose triggering event definition
 * the user has been explicitly granted access to via the GRN grants system.
 */
public record EventDefinitionFilter(boolean isAllAllowed, Set<String> eventDefinitionIds) {

    public static EventDefinitionFilter allAllowed() {
        return new EventDefinitionFilter(true, Set.of());
    }

    public static EventDefinitionFilter allowList(Set<String> eventDefinitionIds) {
        return new EventDefinitionFilter(false, eventDefinitionIds);
    }
}
