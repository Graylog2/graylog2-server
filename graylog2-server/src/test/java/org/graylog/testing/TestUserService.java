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
package org.graylog.testing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.bson.types.ObjectId;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.graylog2.users.UserServiceImplTest;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides an incomplete UserService implementation for tests.
 * It uses fewer dependencies than a full UserServiceImpl
 */
public class TestUserService extends PersistedServiceImpl implements UserService {
    final UserImpl.Factory userFactory;

    public TestUserService(MongoConnection mongoConnection) {
        super(mongoConnection);
        final Permissions permissions = new Permissions(ImmutableSet.of(new RestPermissions()));
        userFactory = new UserServiceImplTest.UserImplFactory(new Configuration(), permissions);
    }

    @Override
    @Nullable
    public User loadById(String id) {
        final DBObject userObject = get(UserImpl.class, id);
        if (userObject == null) {
            return null;
        }
        final Object userId = userObject.get("_id");
        return userFactory.create((ObjectId) userId, userObject.toMap());
    }

    @Override
    public List<User> loadByIds(Collection<String> ids) {
        final DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", ids));
        final List<DBObject> result = query(UserImpl.class, query);

        final List<User> users = Lists.newArrayList();
        for (DBObject dbObject : result) {
            //noinspection unchecked
            users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }

        return users;
    }

    @Nullable
    @Override
    public User load(String username) {
        final DBObject query = new BasicDBObject();
        query.put(UserImpl.USERNAME, username);

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return null;
        }

        if (result.size() > 1) {
            final String msg = "There was more than one matching user for username " + username + ". This should never happen.";
            throw new RuntimeException(msg);
        }

        final DBObject userObject = result.get(0);
        final Object userId = userObject.get("_id");

        return userFactory.create((ObjectId) userId, userObject.toMap());
    }

    @Override
    public Optional<User> loadByAuthServiceUidOrUsername(String authServiceUid, String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int delete(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int deleteById(String userId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public User create() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<User> loadAll() {
        final DBObject query = new BasicDBObject();
        final List<DBObject> result = query(UserImpl.class, query);

        final List<User> users = Lists.newArrayList();
        for (DBObject dbObject : result) {
            users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }

        return users;
    }

    @Override
    public User getAdminUser() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Optional<User> getRootUser() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<User> loadAllForAuthServiceBackend(String authServiceBackendId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<User> loadAllForRole(Role role) {
        final String roleId = role.getId();
        final DBObject query = BasicDBObjectBuilder.start(UserImpl.ROLES, new ObjectId(roleId)).get();

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<User> users = Sets.newHashSetWithExpectedSize(result.size());
        for (DBObject dbObject : result) {
            //noinspection unchecked
            users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }
        return users;
    }

    @Override
    public Set<String> getRoleNames(User user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Permission> getPermissionsForUser(User user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<WildcardPermission> getWildcardPermissionsForUser(User user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<GRNPermission> getGRNPermissionsForUser(User user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> getUserPermissionsFromRoles(User user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void dissociateAllUsersFromRole(Role role) {
        final Collection<User> usersInRole = loadAllForRole(role);
        // remove role from any user still assigned
        for (User user : usersInRole) {
            if (user.isLocalAdmin()) {
                continue;
            }
            final HashSet<String> roles = Sets.newHashSet(user.getRoleIds());
            roles.remove(role.getId());
            user.setRoleIds(roles);
            try {
                save(user);
            } catch (ValidationException e) {
                throw new RuntimeException("Unable to remove role " + role.getName() + " from user " + user, e);
            }
        }
    }
}

