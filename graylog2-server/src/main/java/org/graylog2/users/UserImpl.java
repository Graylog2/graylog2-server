/**
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
import org.apache.shiro.crypto.hash.SimpleHash;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.*;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.firstNonNull;


/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@CollectionName("users")
public class UserImpl extends PersistedImpl implements User {

    private static final Logger LOG = LoggerFactory.getLogger(UserImpl.class);

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String FULL_NAME = "full_name";
    public static final String PERMISSIONS = "permissions";
    public static final String PREFERENCES = "preferences";
    public static final String TIMEZONE = "timezone";
    public static final String EXTERNAL_USER = "external_user";
    public static final String SESSION_TIMEOUT = "session_timeout_ms";

    public static final int MAX_USERNAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_FULL_NAME_LENGTH = 200;

    public UserImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected UserImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    public Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put(USERNAME, new LimitedStringValidator(1, MAX_USERNAME_LENGTH));
            put(PASSWORD, new FilledStringValidator());
            put(EMAIL, new LimitedStringValidator(1, MAX_EMAIL_LENGTH));
            put(FULL_NAME, new LimitedStringValidator(1, MAX_FULL_NAME_LENGTH));
            put(PERMISSIONS, new ListValidator());
        }};
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    @Override
    public String getFullName() {
        return fields.get(FULL_NAME).toString();
    }

    @Override
    public String getName() {
        return fields.get(USERNAME).toString();
    }

    @Override
    public void setName(String username) {
        fields.put(USERNAME, username);
    }

    @Override
    public String getEmail() {
        final Object email = fields.get(EMAIL);
        return email == null ? "" : email.toString();
    }

    @Override
    public List<String> getPermissions() {
        final Object o = fields.get(PERMISSIONS);
        return (List<String>) o;
    }

    @Override
    public Map<String, Object> getPreferences() {
        final Object o = fields.get(PREFERENCES);
        return (Map<String, Object>) o;
    }

    @Override
    public Map<String, String> getStartpage() {
        Map<String, String> startpage = Maps.newHashMap();

        if (fields.containsKey("startpage")) {
            Map<String, String>  obj = (Map<String, String>) fields.get("startpage");
            startpage.put("type", obj.get("type"));
            startpage.put("id", obj.get("id"));
        }

        return startpage;
    }

    @Override
    public long getSessionTimeoutMs() {
        final Object o = fields.get(SESSION_TIMEOUT);
        if (o != null && o instanceof Long) {
            return (Long) o;
        }
        return TimeUnit.HOURS.toMillis(8);
    }

    @Override
    public void setSessionTimeoutMs(long timeoutValue) {
        fields.put(SESSION_TIMEOUT, timeoutValue);
    }

    @Override
    public void setPermissions(List<String> permissions) {
        fields.put(PERMISSIONS, permissions);
    }

    @Override
    public void setPreferences(Map<String, Object> preferences) {
        fields.put(PREFERENCES, preferences);
    }

    @Override
    public void setEmail(String email) {
        fields.put(EMAIL, email);
    }

    @Override
    public void setFullName(String fullname) {
        fields.put(FULL_NAME, fullname);
    }

    @Override
    public String getHashedPassword() {
        return firstNonNull(fields.get(PASSWORD), "").toString();
    }

    public void setHashedPassword(String hashedPassword) {
        fields.put(PASSWORD, hashedPassword);
    }

    @Override
    public void setPassword(String password, String passwordSecret) {
        if (password == null || password.equals("")) {
            // If no password is given, we leave the hashed password empty and we fail during validation.
            setHashedPassword("");
        } else {
            final String newPassword = new SimpleHash("SHA-1", password, passwordSecret).toString();
            setHashedPassword(newPassword);
        }
    }

    @Override
    public boolean isUserPassword(String password, String passwordSecret) {
        final String oldPasswordHash = new SimpleHash("SHA-1", password, passwordSecret).toString();
        return getHashedPassword().equals(oldPasswordHash);
    }

    @Override
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

    @Override
    public void setTimeZone(DateTimeZone timeZone) {
        fields.put(TIMEZONE, timeZone.getID());
    }

    @Override
    public void setTimeZone(String timeZone) {
        fields.put(TIMEZONE, timeZone);
    }

    @Override
    public boolean isExternalUser() {
        return Boolean.valueOf(String.valueOf(fields.get(EXTERNAL_USER)));
    }

    @Override
    public void setExternal(boolean external) {
        fields.put(EXTERNAL_USER, external);
    }

    @Override
    public boolean isLocalAdmin() {
        return false;
    }

    @Override
    public void setStartpage(String type, String id) {
        Map<String, String> startpage = Maps.newHashMap();

        if (type != null && id != null) {
            startpage.put("type", type);
            startpage.put("id", id);
        }

        this.fields.put("startpage", startpage);
    }

    public static class LocalAdminUser extends UserImpl {
        private final Configuration configuration;
        public LocalAdminUser(Configuration configuration) {
            super(null, Maps.<String, Object>newHashMap());
            this.configuration = configuration;
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
            return configuration.getRootUsername();
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
        public Map<String, Object> getPreferences() {
            return Collections.emptyMap();
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
        public boolean isLocalAdmin() {
            return true;
        }
    }
}
