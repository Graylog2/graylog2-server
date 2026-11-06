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

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog2.plugin.database.users.User;

import java.util.Set;

public interface GranteeService {
    Set<Grantee> getAvailableGrantees(User sharingUser);

    Set<Grantee> getModifiableGrantees(Set<Grantee> availableGrantees, ImmutableSet<EntityShareResponse.ActiveShare> activeShares);

    Set<User> getVisibleUsers(User requestingUser);

    /**
     * Filters the given grantees to those the viewer could see in the share dialog. Unlike
     * {@link #getAvailableGrantees(User)}, this only loads the given grantees, so its cost does not depend on the
     * total number of users or teams. The viewer is never part of the result.
     *
     * @param viewer     the user who looks at the grantees
     * @param candidates the grantee GRNs to filter
     * @return the visible grantees
     */
    Set<Grantee> getVisibleGrantees(User viewer, Set<GRN> candidates);

    Set<GRN> getGranteeAliases(GRN grantee);
}
