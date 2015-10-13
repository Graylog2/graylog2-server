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
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class UserServiceImpl extends PersistedServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final Configuration configuration;
    private final RoleService roleService;
    private final UserImpl.Factory userFactory;

    @Inject
    public UserServiceImpl(final MongoConnection mongoConnection,
                           final Configuration configuration,
                           final RoleService roleService,
                           final UserImpl.Factory userFactory) {
        super(mongoConnection);
        this.configuration = configuration;
        this.roleService = roleService;
        this.userFactory = userFactory;
        // ensure that the users' roles array is indexed
        collection(UserImpl.class).createIndex(UserImpl.ROLES);
    }

    @Override
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
        return userFactory.create(Maps.<String, Object>newHashMap());
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
    public User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        UserImpl user = (UserImpl) load(username);

        // create new user object if necessary
        if (user == null) {
            user = userFactory.create(Maps.<String, Object>newHashMap());
        }

        // update user attributes from ldap entry
        updateFromLdap(user, userEntry, ldapSettings, username);

        try {
            save(user);
        } catch (ValidationException e) {
            LOG.error("Cannot save user.", e);
            return null;
        }

        return user;
    }

    @Override
    public void updateFromLdap(User user, LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        final String displayNameAttribute = ldapSettings.getDisplayNameAttribute();
        final String fullName = firstNonNull(userEntry.get(displayNameAttribute), username);

        user.setName(username);
        user.setFullName(fullName);
        user.setExternal(true);

        final String email = userEntry.getEmail();
        if (isNullOrEmpty(email)) {
            LOG.debug("No email address found for user {} in LDAP. Using {}@localhost", username, username);
            user.setEmail(username + "@localhost");
        } else {
            user.setEmail(email);
        }

        // TODO This is a crude hack until we have a proper way to distinguish LDAP users from normal users
        if (isNullOrEmpty(user.getHashedPassword())) {
            ((UserImpl) user).setHashedPassword("User synced from LDAP.");
        }

        if (user.getPermissions() == null) {
            user.setPermissions(Lists.newArrayList(RestPermissions.userSelfEditPermissions(username)));
        } else {
            user.setPermissions(Lists.newArrayList(Sets.union(RestPermissions.userSelfEditPermissions(username),
                                                              Sets.newHashSet(user.getPermissions()))));
        }

        // map ldap groups to user roles, if the mapping is present
        final Set<String> translatedRoleIds = Sets.newHashSet(Sets.union(Sets.newHashSet(ldapSettings.getDefaultGroupId()),
                                                                         ldapSettings.getAdditionalDefaultGroupIds()));
        if (!userEntry.getGroups().isEmpty()) {
            // ldap search returned groups, these always override the ones set on the user
            try {
                final Map<String, Role> roleNameToRole = roleService.loadAllLowercaseNameMap();
                for (String ldapGroupName : userEntry.getGroups()) {
                    final String roleName = ldapSettings.getGroupMapping().get(ldapGroupName);
                    if (roleName == null) {
                        LOG.debug("User {}: No group mapping for ldap group <{}>", username, ldapGroupName);
                        continue;
                    }
                    final Role role = roleNameToRole.get(roleName.toLowerCase(Locale.ENGLISH));
                    if (role != null) {
                        LOG.debug("User {}: Mapping ldap group <{}> to role <{}>", username, ldapGroupName, role.getName());
                        translatedRoleIds.add(role.getId());
                    } else {
                        LOG.warn("User {}: No role found for ldap group <{}>", username, ldapGroupName);
                    }
                }

            } catch (NotFoundException e) {
                LOG.error("Unable to load user roles", e);
            }
        } else if (ldapSettings.getGroupMapping().isEmpty()
                || ldapSettings.getGroupSearchBase().isEmpty()
                || ldapSettings.getGroupSearchPattern().isEmpty()
                || ldapSettings.getGroupIdAttribute().isEmpty()) {
            // no group mapping or configuration set, we'll leave the previously set groups alone on sync
            // when first creating the user these will be empty
            translatedRoleIds.addAll(user.getRoleIds());
        }
        user.setRoleIds(translatedRoleIds);

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
}