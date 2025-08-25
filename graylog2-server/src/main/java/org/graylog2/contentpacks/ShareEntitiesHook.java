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
package org.graylog2.contentpacks;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.UserContext;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.contentpacks.model.ContentPackInstallation;

public class ShareEntitiesHook implements ContentPackInstallationHook {
    private final GRNRegistry grnRegistry;
    private final Provider<EntitySharesService> entitySharesService;

    @Inject
    public ShareEntitiesHook(GRNRegistry grnRegistry, Provider<EntitySharesService> entitySharesService) {
        this.grnRegistry = grnRegistry;
        this.entitySharesService = entitySharesService;
    }

    @Override
    public void afterInstallation(ContentPackInstallation installation, EntityShareRequest shareRequest, UserContext userContext) {
        if (shareRequest.grantees().isEmpty()) {
            return;
        }
        final var user = userContext.getUser();
        final var allEntities = installation.entities();
        final var entityGRNs = allEntities.stream()
                .map(entity -> grnRegistry.newGRN(entity.type().name(), entity.id().id()))
                .toList();
        entityGRNs.forEach((grn) -> entitySharesService.get().updateEntityShares(grn, shareRequest, user));
    }
}
