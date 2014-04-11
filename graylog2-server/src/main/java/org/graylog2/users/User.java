/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */
package org.graylog2.users;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.ListValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.security.RestPermissions;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.firstNonNull;


/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class User extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(User.class);

    private static final String COLLECTION = "users";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String FULL_NAME = "full_name";
    public static final String PERMISSIONS = "permissions";
    public static final String TIMEZONE = "timezone";
    public static final String EXTERNAL_USER = "external_user";
    public static final String SESSION_TIMEOUT = "session_timeout_ms";

    public User(Map<String, Object> fields, Core core) {
        super(core, fields);
    }

    protected User(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);
    }

    public static User load(String username, Core core) {
        LOG.debug("Loading user {}", username);
        // special case for the locally defined user, we don't store that in MongoDB.
        if (core.getConfiguration().getRootUsername().equals(username)) {
            LOG.debug("User {} is the built-in admin user", username);
            return new LocalAdminUser(core);
        }

        DBObject query = new BasicDBObject();
        query.put(USERNAME, username);

        List<DBObject> result = query(query, core, COLLECTION);

        if (result == null)     { return null; }
        if (result.size() == 0) { return null; }

        if (result.size() > 1) {
            LOG.error("There was more than one matching user. This should never happen.");
            throw new RuntimeException("There was more than one matching user. This should never happen.");
        }
        final DBObject userObject = result.get(0);

        final Object userId = userObject.get("_id");
        LOG.debug("Loaded user {}/{}from MongoDB", username, userId);
        return new User((ObjectId) userId, userObject.toMap(), core);
    }

    public static List<User> loadAll(Core core) {
        List<User> users = Lists.newArrayList();

        DBObject query = new BasicDBObject();
        List<DBObject> result = query(query, core, COLLECTION);

        for (DBObject dbObject : result) {
            users.add(new User((ObjectId) dbObject.get("_id"), dbObject.toMap(), core));
        }
        return users;
    }

    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put(USERNAME, new FilledStringValidator());
            put(PASSWORD, new OptionalStringValidator());
            put(EMAIL, new OptionalStringValidator());
            put(FULL_NAME, new FilledStringValidator());
            put(PERMISSIONS, new ListValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    public String getFullName() {
        return fields.get(FULL_NAME).toString();
    }

    public String getName() {
        return fields.get(USERNAME).toString();
    }

    public void setName(String username) {
        fields.put(USERNAME, username);
    }

    public String getEmail() {
        final Object email = fields.get(EMAIL);
        return email == null ? "" : email.toString();
    }

    public List<String> getPermissions() {
        final Object o = fields.get(PERMISSIONS);
        return (List<String>) o;
    }

    public Map<String, String> getStartpage() {
        Map<String, String> startpage = Maps.newHashMap();

        if (fields.containsKey("startpage")) {
            Map<String, String>  obj = (Map<String, String>) fields.get("startpage");
            startpage.put("type", obj.get("type"));
            startpage.put("id", obj.get("id"));
        }

        return startpage;
    }

    public long getSessionTimeoutMs() {
        final Object o = fields.get(SESSION_TIMEOUT);
        if (o != null && o instanceof Long) {
            return (Long) o;
        }
        return TimeUnit.HOURS.toMillis(8);
    }

    public void setSessionTimeoutMs(long timeoutValue) {
        fields.put(SESSION_TIMEOUT, timeoutValue);
    }

    public void setPermissions(List<String> permissions) {
        fields.put(PERMISSIONS, permissions);
    }

    public void setEmail(String email) {
        fields.put(EMAIL, email);
    }

    public void setFullName(String fullname) {
        fields.put(FULL_NAME, fullname);
    }

    public String getHashedPassword() {
        return firstNonNull(fields.get(PASSWORD), "").toString();
    }

    public void setHashedPassword(String hashedPassword) {
        fields.put(PASSWORD, hashedPassword);
    }

    public DateTimeZone getTimeZone() {
        final Object o = fields.get(TIMEZONE);
        try {
            if (o != null) {
                return DateTimeZone.forID(o.toString());
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid timezone {} saved for user {}", o.toString(), getName());
        }
        return null;
    }

    public void setTimeZone(DateTimeZone timeZone) {
        fields.put(TIMEZONE, timeZone == null ? null : timeZone.getID());
    }

    public boolean isExternalUser() {
        return Boolean.valueOf(String.valueOf(fields.get(EXTERNAL_USER)));
    }

    public void setExternal(boolean external) {
        fields.put(EXTERNAL_USER, external);
    }

    public void setStartpage(String type, String id) {
        Map<String, String> startpage = Maps.newHashMap();

        if (type != null && id != null) {
            startpage.put("type", type);
            startpage.put("id", id);
        }

        this.fields.put("startpage", startpage);
    }

    public static User syncFromLdapEntry(Core core, LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        User user = load(username, core);
        // create new user object if necessary
        if (user == null) {
            Map<String, Object> fields = Maps.newHashMap();
            user = new User(fields, core);
        }
        // update user attributes from ldap entry
        user.updateFromLdap(userEntry, ldapSettings, username);
        try {
            user.save();
        } catch (ValidationException e) {
            LOG.error("Cannot save user.", e);
            return null;
        }
        return user;
    }

    public void updateFromLdap(LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        final String displayNameAttribute = ldapSettings.getDisplayNameAttribute();
        final String fullname = userEntry.get(displayNameAttribute);
        setFullName(fullname);
        setName(username);
        setExternal(true);
        setEmail(userEntry.getEmail());

        // only touch the permissions if none existed for this account before
        // i.e. only determine the new permissions for an account on initially importing it.
        if (getPermissions() == null) {
            if (ldapSettings.getDefaultGroup().equals("reader")) {
                setPermissions(Lists.newArrayList(RestPermissions.readerPermissions(username)));

            } else {
                setPermissions(Lists.<String>newArrayList("*"));
            }
        }
    }

    public static class LocalAdminUser extends User {
        public LocalAdminUser(Core core) {
            super(null, Maps.<String, Object>newHashMap(), core);
        }

        @Override
        public String getId() {
            return "local:admin";
        }

        @Override
        public String getFullName() {
            return "Administrator";
        }

        public String getEmail() {
            return "none";
        }

        @Override
        public String getName() {
            return core.getConfiguration().getRootUsername();
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public boolean isExternalUser() {
            return false;
        }

        @Override
        public List<String> getPermissions() {
            return Lists.newArrayList("*");
        }

        @Override
        public long getSessionTimeoutMs() {
            return TimeUnit.HOURS.toMillis(8);
        }

        @Override
        public DateTimeZone getTimeZone() {
            return null;
        }

        @Override
        public ObjectId save() throws ValidationException {
            throw new IllegalStateException("Cannot modify local root user, this is a bug.");
        }
    }
}
