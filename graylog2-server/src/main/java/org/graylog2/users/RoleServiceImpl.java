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
package org.graylog2.users;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoDBUpsertRetryer;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mongojack.DBQuery.and;
import static org.mongojack.DBQuery.is;

public class RoleServiceImpl implements RoleService {
    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private static final String ROLES = "roles";
    private static final String NAME_LOWER = "name_lower";
    private static final String READ_ONLY = "read_only";
    private static final String ID = "_id";

    private static final String ADMIN_ROLENAME = "Admin";
    private static final String READER_ROLENAME = "Reader";

    private final JacksonDBCollection<RoleImpl, ObjectId> dbCollection;
    private final Validator validator;
    private final String adminRoleObjectId;
    private final String readerRoleObjectId;

    @Inject
    public RoleServiceImpl(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper,
                              Permissions permissions,
                              Validator validator) {
        this.validator = validator;

        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(ROLES),
                RoleImpl.class,
                ObjectId.class,
                mapper.get());
        // lower case role names are unique, this allows arbitrary naming, but still uses an index
        dbCollection.createIndex(new BasicDBObject(NAME_LOWER, 1), new BasicDBObject("unique", true));

        // make sure the two built-in roles actually exist
        adminRoleObjectId = checkNotNull(ensureBuiltinRole(ADMIN_ROLENAME, Sets.newHashSet("*"), "Admin",
                                                           "Grants all permissions for Graylog administrators (built-in)"));
        readerRoleObjectId = checkNotNull(ensureBuiltinRole(READER_ROLENAME, permissions.readerBasePermissions(), "Reader",
                          "Grants basic permissions for every Graylog user (built-in)"));

    }

    @Nullable
    private String ensureBuiltinRole(String roleName,
                                     Set<String> expectedPermissions,
                                     String name, String description) {
        RoleImpl previousRole = null;
        try {
            previousRole = load(roleName);
            if (!previousRole.isReadOnly() || !expectedPermissions.equals(previousRole.getPermissions()))  {
                final String msg = "Invalid role '" + roleName + "', fixing it.";
                log.error(msg);
                throw new IllegalArgumentException(msg); // jump to fix code
            }
        } catch (NotFoundException | IllegalArgumentException | NoSuchElementException ignored) {
            log.info("{} role is missing or invalid, re-adding it as a built-in role.", roleName);
            final RoleImpl fixedAdmin = new RoleImpl();
            // copy the mongodb id over, in order to update the role instead of readding it
            if (previousRole != null) {
                fixedAdmin._id = previousRole._id;
            }
            fixedAdmin.setReadOnly(true);
            fixedAdmin.setName(name);
            fixedAdmin.setDescription(description);
            fixedAdmin.setPermissions(expectedPermissions);

            try {
                final RoleImpl savedRole = save(fixedAdmin);
                return savedRole.getId();
            } catch (DuplicateKeyException | ValidationException e) {
                log.error("Unable to save fixed " + roleName + " role, please restart Graylog to fix this.", e);
            }
        }

        if (previousRole == null) {
            log.error("Unable to access fixed " + roleName + " role, please restart Graylog to fix this.");
            return null;
        }

        return previousRole.getId();
    }

    @Override
    public Role loadById(String roleId) throws NotFoundException {
        final Role role = dbCollection.findOneById(new ObjectId(roleId));
        if (role == null) {
            throw new NotFoundException("No role found with id " + roleId);
        }
        return role;
    }

    @Override
    public RoleImpl load(String roleName) throws NotFoundException {
        final RoleImpl role = dbCollection.findOne(is(NAME_LOWER, roleName.toLowerCase(Locale.ENGLISH)));

        if (role == null) {
            throw new NotFoundException("No role found with name " + roleName);
        }
        return role;
    }

    @Override
    public boolean exists(String roleName) {
        return dbCollection.getCount(is(NAME_LOWER, roleName.toLowerCase(Locale.ENGLISH))) == 1;
    }

    @Override
    public Set<Role> loadAll() {
        try (DBCursor<RoleImpl> rolesCursor = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterable<? extends Role>) rolesCursor);
        }
    }

    @Override
    public Map<String, Role> findIdMap(Set<String> roleIds) throws NotFoundException {
        final DBQuery.Query query = DBQuery.in(ID, roleIds);
        try (DBCursor<RoleImpl> rolesCursor = dbCollection.find(query)) {
            ImmutableSet<Role> roles = ImmutableSet.copyOf((Iterable<? extends Role>) rolesCursor);
            return Maps.uniqueIndex(roles, new Function<Role, String>() {
                @Nullable
                @Override
                public String apply(Role input) {
                    return input.getId();
                }
            });
        }
    }

    @Override
    public Map<String, Role> loadAllIdMap() throws NotFoundException {
        final Set<Role> roles = loadAll();
        return Maps.uniqueIndex(roles, new Function<Role, String>() {
            @Nullable
            @Override
            public String apply(Role input) {
                return input.getId();
            }
        });
    }

    @Override
    public Map<String, Role> loadAllLowercaseNameMap() throws NotFoundException {
        final Set<Role> roles = loadAll();
        return Maps.uniqueIndex(roles, Roles.roleToNameFunction(true));
    }

    @Override
    public RoleImpl save(Role role1) throws ValidationException {
        // sucky but necessary because of graylog2-shared not knowing about mongodb :(
        if (!(role1 instanceof RoleImpl)) {
            throw new IllegalArgumentException("invalid Role implementation class");
        }
        RoleImpl role = (RoleImpl) role1;
        final Set<ConstraintViolation<Role>> violations = validate(role);
        if (!violations.isEmpty()) {
            throw new ValidationException("Validation failed.", violations.toString());
        }
        return MongoDBUpsertRetryer.run(() ->
                dbCollection.findAndModify(is(NAME_LOWER, role.nameLower()), null, null, false, role, true, true));
    }

    @Override
    public Set<ConstraintViolation<Role>> validate(Role role) {
        return validator.validate(role);
    }

    @Override
    public int delete(String roleName) {
        final DBQuery.Query nameMatchesAndNotReadonly = and(is(READ_ONLY, false), is(NAME_LOWER, roleName.toLowerCase(Locale.ENGLISH)));
        return dbCollection.remove(nameMatchesAndNotReadonly).getN();
    }

    @Override
    public String getAdminRoleObjectId() {
        return adminRoleObjectId;
    }

    @Override
    public String getReaderRoleObjectId() {
        return readerRoleObjectId;
    }
}
