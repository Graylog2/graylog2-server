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

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.events.UserChangedEvent;
import org.graylog2.users.events.UserDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class UserServiceImpl extends PersistedServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final Configuration configuration;
    private final RoleService roleService;
    private final AccessTokenService accesstokenService;
    private final UserImpl.Factory userFactory;
    private final InMemoryRolePermissionResolver inMemoryRolePermissionResolver;
    private final EventBus serverEventBus;
    private final GRNRegistry grnRegistry;
    private final PermissionAndRoleResolver permissionAndRoleResolver;

    @Inject
    public UserServiceImpl(final MongoConnection mongoConnection,
                           final Configuration configuration,
                           final RoleService roleService,
                           final AccessTokenService accessTokenService,
                           final UserImpl.Factory userFactory,
                           final InMemoryRolePermissionResolver inMemoryRolePermissionResolver,
                           final EventBus serverEventBus,
                           final GRNRegistry grnRegistry,
                           final PermissionAndRoleResolver permissionAndRoleResolver
    ) {
        super(mongoConnection);
        this.configuration = configuration;
        this.roleService = roleService;
        this.accesstokenService = accessTokenService;
        this.userFactory = userFactory;
        this.inMemoryRolePermissionResolver = inMemoryRolePermissionResolver;
        this.serverEventBus = serverEventBus;
        this.grnRegistry = grnRegistry;
        this.permissionAndRoleResolver = permissionAndRoleResolver;

        // ensure that the users' roles array is indexed
        collection(UserImpl.class).createIndex(UserImpl.ROLES);
    }

    @Override
    @Nullable
    public User loadById(final String id) {
        // special case for the locally defined user, we don't store that in MongoDB.
        if (!configuration.isRootUserDisabled() && id.equals(UserImpl.LocalAdminUser.LOCAL_ADMIN_ID)) {
            LOG.debug("User {} is the built-in admin user", id);
            return userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId());
        }
        final DBObject userObject = get(UserImpl.class, id);
        if (userObject == null) {
            return null;
        }
        final Object userId = userObject.get("_id");
        return userFactory.create((ObjectId) userId, userObject.toMap());
    }

    @Override
    public List<User> loadByIds(Collection<String> ids) {
        final HashSet<String> userIds = new HashSet<>(ids);
        final List<User> users = new ArrayList<>();

        // special case for the locally defined user, we don't store that in MongoDB.
        if (!configuration.isRootUserDisabled() && userIds.stream().anyMatch(UserImpl.LocalAdminUser.LOCAL_ADMIN_ID::equals)) {
            // The local admin ID is not a valid ObjectId so we have to remove it from the query
            userIds.remove(UserImpl.LocalAdminUser.LOCAL_ADMIN_ID);
            users.add(userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId()));
        }

        final DBObject query = new BasicDBObject();
        query.put("_id", new BasicDBObject("$in", userIds.stream().map(ObjectId::new).collect(Collectors.toSet())));

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return users;
        }

        for (final DBObject dbObject : result) {
            //noinspection unchecked
            users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }

        return users;
    }

    @Override
    @Nullable
    public User load(final String username) {
        LOG.debug("Loading user {}", username);

        // special case for the locally defined user, we don't store that in MongoDB.
        if (!configuration.isRootUserDisabled() && configuration.getRootUsername().equals(username)) {
            LOG.debug("User {} is the built-in admin user", username);
            return userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId());
        }

        final DBObject query = new BasicDBObject();
        query.put(UserImpl.USERNAME, username);

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return null;
        }

        if (result.size() > 1) {
            final String msg = "There was more than one matching user for username " + username + ". This should never happen.";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        final DBObject userObject = result.get(0);
        final Object userId = userObject.get("_id");

        LOG.debug("Loaded user {}/{} from MongoDB", username, userId);
        return userFactory.create((ObjectId) userId, userObject.toMap());
    }

    @Override
    public Optional<User> loadByAuthServiceUidOrUsername(String authServiceUid, String username) {
        checkArgument(!isBlank(authServiceUid), "authServiceUid cannot be blank");
        checkArgument(!isBlank(username), "username cannot be blank");

        LOG.debug("Loading user by auth service UID <{}> or username <{}>", authServiceUid, username);

        // special case for the locally defined user, we don't store that in MongoDB.
        if (!configuration.isRootUserDisabled() && configuration.getRootUsername().equals(username)) {
            LOG.debug("User <{}> is the built-in admin user", username);
            return Optional.ofNullable(userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId()));
        }

        final DBObject query = new BasicDBObject("$or", ImmutableList.of(
                new BasicDBObject(UserImpl.AUTH_SERVICE_UID, authServiceUid),
                new BasicDBObject(UserImpl.USERNAME, username)
        ));

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        if (result.size() > 1) {
            final String msg = "There was more than one matching user for auth service UID <" + authServiceUid + "> or username <" + username + ">. This should never happen.";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        final DBObject userObject = result.get(0);
        final Object userId = userObject.get("_id");

        LOG.debug("Loaded user {}/{}/{} from MongoDB", authServiceUid, username, userId);
        return Optional.ofNullable(userFactory.create((ObjectId) userId, userObject.toMap()));
    }

    @Override
    public int delete(final String username) {
        DBObject query = new BasicDBObject();
        query.put(UserImpl.USERNAME, username);

        final List<DBObject> result = query(UserImpl.class, query);
        if (result == null || result.isEmpty()) {
            return 0;
        }

        final ImmutableList.Builder<UserDeletedEvent> deletedUsers = ImmutableList.builder();
        result.forEach(userObject -> {
            final ObjectId userId = (ObjectId) userObject.get("_id");
            deletedUsers.add(UserDeletedEvent.create(userId.toHexString(), username));
        });

        LOG.debug("Deleting user(s) with username \"{}\"", username);
        query = BasicDBObjectBuilder.start(UserImpl.USERNAME, username).get();
        final int deleteCount = destroy(query, UserImpl.COLLECTION_NAME);

        if (deleteCount > 1) {
            LOG.warn("Removed {} users matching username \"{}\".", deleteCount, username);
        }
        accesstokenService.deleteAllForUser(username);
        deletedUsers.build().forEach(serverEventBus::post);
        return deleteCount;
    }

    @Override
    public int deleteById(final String userId) {
        final User user = loadById(userId);
        if (user == null) {
            return 0;
        }
        DBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(userId));
        final int deleteCount = destroy(query, UserImpl.COLLECTION_NAME);
        accesstokenService.deleteAllForUser(user.getName());
        serverEventBus.post(UserDeletedEvent.create(userId, user.getName()));
        return deleteCount;
    }

    @Override
    public User create() {
        return userFactory.create(initialUserFields());
    }

    public static Map<String, Object> initialUserFields() {
        final Map<String, Object> fields = new HashMap<>();

        // We always want the authentication service fields in new user objects, event if they don't get set
        fields.put(UserImpl.AUTH_SERVICE_ID, null);
        fields.put(UserImpl.AUTH_SERVICE_UID, null);
        // User objects are internal by default. Ensure that we set this fields on all user objects.
        fields.put(UserImpl.EXTERNAL_USER, false);
        // New accounts are enabled by default
        fields.put(UserImpl.ACCOUNT_STATUS, User.AccountStatus.ENABLED.toString().toLowerCase(Locale.US));
        return fields;
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
    public <T extends Persisted> String save(T model) throws ValidationException {
        if (model instanceof UserImpl.LocalAdminUser) {
            throw new IllegalStateException("Cannot modify local root user, this is a bug.");
        }

        final String userId = super.save(model);

        serverEventBus.post(UserChangedEvent.create(userId));

        return userId;
    }

    @Override
    @Deprecated
    public User getAdminUser() {
        return getRootUser().orElseThrow(() ->
                new IllegalStateException("Local admin user requested but root user is disabled in config."));
    }

    @Override
    public Optional<User> getRootUser() {
        if (configuration.isRootUserDisabled()) {
            return Optional.empty();
        }
        return Optional.of(userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId()));
    }

    @Override
    public long count() {
        return totalCount(UserImpl.class);
    }

    @Override
    public List<User> loadAllForAuthServiceBackend(String authServiceBackendId) {
        final DBObject query = BasicDBObjectBuilder.start(UserImpl.AUTH_SERVICE_ID, authServiceBackendId).get();
        final List<DBObject> result = query(UserImpl.class, query);

        final List<User> users = Lists.newArrayList();
        for (DBObject dbObject : result) {
            //noinspection unchecked
            users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }

        return users;
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
        final Set<String> roleIds = user.getRoleIds();

        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        Map<String, Role> idMap;
        try {
            idMap = roleService.loadAllIdMap();
        } catch (NotFoundException e) {
            LOG.error("Unable to load role ID map. Using empty map.", e);
            idMap = Collections.emptyMap();
        }

        return ImmutableSet.copyOf(
                Iterables.filter(
                        Collections2.transform(roleIds, Roles.roleIdToNameFunction(idMap)),
                        Predicates.notNull()
                )
        );
    }

    @Override
    public List<Permission> getPermissionsForUser(User user) {
        final GRN principal = grnRegistry.ofUser(user);
        final ImmutableSet.Builder<Permission> permSet = ImmutableSet.<Permission>builder()
                .addAll(user.getPermissions().stream().map(CaseSensitiveWildcardPermission::new).collect(Collectors.toSet()))
                .addAll(permissionAndRoleResolver.resolvePermissionsForPrincipal(principal))
                .addAll(getUserPermissionsFromRoles(user).stream().map(CaseSensitiveWildcardPermission::new).collect(Collectors.toSet()));

        return permSet.build().asList();
    }

    @Override
    public List<WildcardPermission> getWildcardPermissionsForUser(User user) {
        return getPermissionsForUser(user).stream()
                .filter(WildcardPermission.class::isInstance).map(WildcardPermission.class::cast).collect(Collectors.toList());
    }

    @Override
    public List<GRNPermission> getGRNPermissionsForUser(User user) {
        return getPermissionsForUser(user).stream()
                .filter(GRNPermission.class::isInstance).map(GRNPermission.class::cast).collect(Collectors.toList());
    }

    @Override
    public Set<String> getUserPermissionsFromRoles(User user) {
        final ImmutableSet.Builder<String> permSet = ImmutableSet.builder();

        for (String roleId : user.getRoleIds()) {
            permSet.addAll(inMemoryRolePermissionResolver.resolveStringPermission(roleId));
        }
        permissionAndRoleResolver.resolveRolesForPrincipal(grnRegistry.ofUser(user)).forEach(roleId ->
                permSet.addAll(inMemoryRolePermissionResolver.resolveStringPermission(roleId))
        );

        return permSet.build();
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
                LOG.error("Unable to remove role {} from user {}", role.getName(), user);
            }
        }
    }
}
