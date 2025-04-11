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

import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNType;
import org.graylog.security.Capability;
import org.graylog2.plugin.database.users.User;

import java.util.Map;
import java.util.Optional;

public interface EntitySharesService {
    EntityShareResponse prepareShare(Optional<GRN> ownedEntity, EntityShareRequest request, User sharingUser, Subject sharingSubject);


    EntityShareResponse updateEntityShares(GRNType grnType, String id, EntityShareRequest request, User sharingUser);
    EntityShareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser);

    Map<GRN, Capability> getGrants(GRN ownedEntity);
}
