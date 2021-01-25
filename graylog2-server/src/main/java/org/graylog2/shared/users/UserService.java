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
package org.graylog2.shared.users;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserService extends PersistedService {
    @Nullable
    User load(String username);

    @Nullable
    User loadById(String id);

    List<User> loadByIds(Collection<String> ids);

    /**
     * Tries to find a user for the given authentication service UID or username. (in that order)
     *
     * @param authServiceUid the authentication service UID (tried first)
     * @param username       the username (tried second)
     * @return the user or an empty option if no user can be found
     */
    Optional<User> loadByAuthServiceUidOrUsername(String authServiceUid, String username);

    int delete(String username);

    int deleteById(String userId);

    User create();

    List<User> loadAll();

    /**
     * @deprecated If you <b>really</b> need the root user, use {@link UserService#getRootUser()} instead.
     */
    @Deprecated
    User getAdminUser();

    /**
     * Get the root user. The root user might not be present in all environments and there shouldn't really be
     * a need to explicitly refer to the root user. But if you really need it, here you go.
     *
     * @return The root user, if present. An empty optional otherwise.
     */
    Optional<User> getRootUser();

    long count();

    List<User> loadAllForAuthServiceBackend(String authServiceBackendId);

    Collection<User> loadAllForRole(Role role);

    Set<String> getRoleNames(User user);

    List<Permission> getPermissionsForUser(User user);

    List<WildcardPermission> getWildcardPermissionsForUser(User user);

    List<GRNPermission> getGRNPermissionsForUser(User user);

    Set<String> getUserPermissionsFromRoles(User user);

    void dissociateAllUsersFromRole(Role role);
}
