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
package org.graylog.security.shares;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;

import java.util.List;

/**
 * Lightweight grant lookup. Kept separate from {@link EntitySharesService} so that callers
 * (e.g. content pack facades reachable via {@link org.graylog.security.entities.EntityDependencyResolver})
 * can resolve grants without dragging in the full sharing dependency graph and creating a Guice cycle.
 */
@Singleton
public class EntityGrantLookup {
    private final GRNRegistry grnRegistry;
    private final DBGrantService grantService;

    @Inject
    public EntityGrantLookup(GRNRegistry grnRegistry, DBGrantService grantService) {
        this.grnRegistry = grnRegistry;
        this.grantService = grantService;
    }

    public List<GrantDTO> getGrantsForTarget(GRNType type, String id) {
        final GRN grn = grnRegistry.newGRN(type, id);
        return grantService.getForTarget(grn);
    }
}
