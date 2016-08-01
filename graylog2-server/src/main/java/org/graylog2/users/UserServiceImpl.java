/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.users;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserServiceImpl extends PersistedServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final Configuration configuration;
    private final RoleService roleService;
    private final UserImpl.Factory userFactory;
    private final InMemoryRolePermissionResolver inMemoryRolePermissionResolver;

    @Inject
    public UserServiceImpl(final MongoConnection mongoConnection,
                           final Configuration configuration,
                           final RoleService roleService,
                           final UserImpl.Factory userFactory,
                           final InMemoryRolePermissionResolver inMemoryRolePermissionResolver) {
        super(mongoConnection);
        this.configuration = configuration;
        this.roleService = roleService;
        this.userFactory = userFactory;
        this.inMemoryRolePermissionResolver = inMemoryRolePermissionResolver;

        // ensure that the users' roles array is indexed
        collection(UserImpl.class).createIndex(UserImpl.ROLES);
    }

    @Override
    @Nullable
    public User load(final String username) {
        LOG.debug("Loading user {}", username);

        // special case for the locally defined user, we don't store that in MongoDB.
        if (configuration.getRootUsername().equals(username)) {
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
    public int delete(final String username) {
        LOG.debug("Deleting user(s) with username \"{}\"", username);
        final DBObject query = BasicDBObjectBuilder.start(UserImpl.USERNAME, username).get();
        final int result = destroy(query, UserImpl.COLLECTION_NAME);

        if (result > 1) {
            LOG.warn("Removed {} users matching username \"{}\".", result, username);
        }

        return result;
    }

    @Override
    public User create() {
        return userFactory.create(Maps.newHashMap());
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

        return super.save(model);
    }

    @Override
    public User getAdminUser() {
        return userFactory.createLocalAdminUser(roleService.getAdminRoleObjectId());
    }

    @Override
    public long count() {
        return totalCount(UserImpl.class);
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
    public List<String> getPermissionsForUser(User user) {
        final ImmutableSet.Builder<String> permSet = ImmutableSet.<String>builder()
                .addAll(user.getPermissions())
                .addAll(getUserPermissionsFromRoles(user));

        return permSet.build().asList();
    }

    @Override
    public Set<String> getUserPermissionsFromRoles(User user) {
        final ImmutableSet.Builder<String> permSet = ImmutableSet.builder();

        for (String roleId : user.getRoleIds()) {
            permSet.addAll(inMemoryRolePermissionResolver.resolveStringPermission(roleId));
        }

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
