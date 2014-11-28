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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.LimitedOptionalStringValidator;
import org.graylog2.database.validators.LimitedStringValidator;
import org.graylog2.database.validators.ListValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

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

    public UserImpl(final Map<String, Object> fields) {
        super(fields);
    }

    protected UserImpl(final ObjectId id, final Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(USERNAME, new LimitedStringValidator(1, MAX_USERNAME_LENGTH))
                .put(PASSWORD, new FilledStringValidator())
                .put(EMAIL, new LimitedStringValidator(1, MAX_EMAIL_LENGTH))
                .put(FULL_NAME, new LimitedOptionalStringValidator(MAX_FULL_NAME_LENGTH))
                .put(PERMISSIONS, new ListValidator())
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(final String key) {
        return Collections.emptyMap();
    }

    @Override
    public String getFullName() {
        return fields.get(FULL_NAME).toString();
    }

    @Override
    public void setFullName(final String fullname) {
        fields.put(FULL_NAME, fullname);
    }

    @Override
    public String getName() {
        return String.valueOf(fields.get(USERNAME));
    }

    @Override
    public void setName(final String username) {
        fields.put(USERNAME, username);
    }

    @Override
    public String getEmail() {
        final Object email = fields.get(EMAIL);
        return email == null ? "" : email.toString();
    }

    @Override
    public void setEmail(final String email) {
        fields.put(EMAIL, email);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getPermissions() {
        return (List<String>) fields.get(PERMISSIONS);
    }

    @Override
    public void setPermissions(final List<String> permissions) {
        fields.put(PERMISSIONS, permissions);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPreferences() {
        return (Map<String, Object>) fields.get(PREFERENCES);
    }

    @Override
    public void setPreferences(final Map<String, Object> preferences) {
        fields.put(PREFERENCES, preferences);
    }

    @Override
    public Map<String, String> getStartpage() {
        final Map<String, String> startpage = Maps.newHashMap();

        if (fields.containsKey("startpage")) {
            @SuppressWarnings("unchecked")
            final Map<String, String> obj = (Map<String, String>) fields.get("startpage");
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
    public void setSessionTimeoutMs(final long timeoutValue) {
        fields.put(SESSION_TIMEOUT, timeoutValue);
    }

    @Override
    public String getHashedPassword() {
        return firstNonNull(fields.get(PASSWORD), "").toString();
    }

    public void setHashedPassword(final String hashedPassword) {
        fields.put(PASSWORD, hashedPassword);
    }

    @Override
    public void setPassword(final String password, final String passwordSecret) {
        if (password == null || "".equals(password)) {
            // If no password is given, we leave the hashed password empty and we fail during validation.
            setHashedPassword("");
        } else {
            final String newPassword = new SimpleHash("SHA-1", password, passwordSecret).toString();
            setHashedPassword(newPassword);
        }
    }

    @Override
    public boolean isUserPassword(final String password, final String passwordSecret) {
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
    public void setTimeZone(final String timeZone) {
        fields.put(TIMEZONE, timeZone);
    }

    @Override
    public void setTimeZone(final DateTimeZone timeZone) {
        fields.put(TIMEZONE, timeZone.getID());
    }

    @Override
    public boolean isExternalUser() {
        return Boolean.valueOf(String.valueOf(fields.get(EXTERNAL_USER)));
    }

    @Override
    public void setExternal(final boolean external) {
        fields.put(EXTERNAL_USER, external);
    }

    @Override
    public boolean isLocalAdmin() {
        return false;
    }

    @Override
    public void setStartpage(final String type, final String id) {
        final Map<String, String> startpage = Maps.newHashMap();

        if (type != null && id != null) {
            startpage.put("type", type);
            startpage.put("id", id);
        }

        this.fields.put("startpage", startpage);
    }

    public static class LocalAdminUser extends UserImpl {
        private final Configuration configuration;

        public LocalAdminUser(Configuration configuration) {
            super(null, Collections.<String, Object>emptyMap());
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
            return Collections.singletonList("*");
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
