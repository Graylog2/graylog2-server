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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.shiro.subject.Subject;
import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.PermissionAndRoleResolver;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;

public class EventDefinitionFilterFactory {
    private final EventsConfigurationProvider eventsConfiguration;
    private final DBGrantService grantService;
    private final GRNRegistry grnRegistry;
    private final PermissionAndRoleResolver permissionAndRoleResolver;

    @Inject
    public EventDefinitionFilterFactory(EventsConfigurationProvider eventsConfiguration,
                                        DBGrantService grantService,
                                        GRNRegistry grnRegistry,
                                        PermissionAndRoleResolver permissionAndRoleResolver) {
        this.eventsConfiguration = eventsConfiguration;
        this.grantService = grantService;
        this.grnRegistry = grnRegistry;
        this.permissionAndRoleResolver = permissionAndRoleResolver;
    }

    public EventDefinitionFilter forSubject(Subject subject) {
        if (!eventsConfiguration.get().enforceEventDefinitionPermissions() || subject.isPermitted(EVENT_DEFINITIONS_READ)) {
            return EventDefinitionFilter.allAllowed();
        }
        final GRN userGRN = grnRegistry.newGRN(GRNTypes.USER, (String) subject.getPrincipal());
        final Set<GRN> grantees = permissionAndRoleResolver.resolveGrantees(userGRN);
        final Set<String> ids = grantService.getForGranteesOrGlobal(grantees).stream()
                .map(GrantDTO::target)
                .filter(target -> target.isType(GRNTypes.EVENT_DEFINITION))
                .map(GRN::entity)
                .collect(Collectors.toSet());
        return EventDefinitionFilter.allowList(ids);
    }
}
