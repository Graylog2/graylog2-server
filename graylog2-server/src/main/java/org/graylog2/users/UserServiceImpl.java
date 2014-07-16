/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.users;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.security.RestPermissions;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class UserServiceImpl extends PersistedServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final Configuration configuration;

    @Inject
    public UserServiceImpl(MongoConnection mongoConnection, Configuration configuration) {
        super(mongoConnection);
        this.configuration = configuration;
    }

    @Override
    public User load(String username) {
        LOG.debug("Loading user {}", username);
        // special case for the locally defined user, we don't store that in MongoDB.
        if (configuration.getRootUsername().equals(username)) {
            LOG.debug("User {} is the built-in admin user", username);
            return new UserImpl.LocalAdminUser(configuration);
        }

        DBObject query = new BasicDBObject();
        query.put(UserImpl.USERNAME, username);

        List<DBObject> result = query(UserImpl.class, query);

        if (result == null) {
            return null;
        }
        if (result.size() == 0) {
            return null;
        }

        if (result.size() > 1) {
            LOG.error("There was more than one matching user. This should never happen.");
            throw new RuntimeException("There was more than one matching user. This should never happen.");
        }
        final DBObject userObject = result.get(0);

        final Object userId = userObject.get("_id");
        LOG.debug("Loaded user {}/{}from MongoDB", username, userId);
        return new UserImpl((ObjectId) userId, userObject.toMap());
    }

    @Override
    public User create() {
        Map<String, Object> fields = Maps.newHashMap();
        return new UserImpl(fields);
    }

    @Override
    public List<User> loadAll() {
        List<User> users = Lists.newArrayList();

        DBObject query = new BasicDBObject();
        List<DBObject> result = query(UserImpl.class, query);

        for (DBObject dbObject : result) {
            users.add(new UserImpl((ObjectId) dbObject.get("_id"), dbObject.toMap()));
        }
        return users;
    }

    @Override
    public User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        User user = load(username);
        // create new user object if necessary
        if (user == null) {
            Map<String, Object> fields = Maps.newHashMap();
            user = new UserImpl(fields);
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
        final String fullname = userEntry.get(displayNameAttribute);
        user.setFullName(fullname);
        user.setName(username);
        user.setExternal(true);
        user.setEmail(userEntry.getEmail());

        // only touch the permissions if none existed for this account before
        // i.e. only determine the new permissions for an account on initially importing it.
        if (user.getPermissions() == null) {
            if (ldapSettings.getDefaultGroup().equals("reader")) {
                user.setPermissions(Lists.newArrayList(RestPermissions.readerPermissions(username)));

            } else {
                user.setPermissions(Lists.<String>newArrayList("*"));
            }
        }
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        if (model instanceof UserImpl.LocalAdminUser)
            throw new IllegalStateException("Cannot modify local root user, this is a bug.");
        return super.save(model);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User getAdminUser() {
        return new UserImpl.LocalAdminUser(configuration);
    }
}