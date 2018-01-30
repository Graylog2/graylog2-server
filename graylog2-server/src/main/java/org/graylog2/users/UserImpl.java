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

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.CollectionName;
import org.graylog2.database.ObjectIdStringFunction;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.StringObjectIdFunction;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.LimitedOptionalStringValidator;
import org.graylog2.database.validators.LimitedStringValidator;
import org.graylog2.database.validators.ListValidator;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

@CollectionName(UserImpl.COLLECTION_NAME)
public class UserImpl extends PersistedImpl implements User {
    private final PasswordAlgorithmFactory passwordAlgorithmFactory;
    private final Permissions permissions;

    public interface Factory {
        UserImpl create(final Map<String, Object> fields);

        UserImpl create(final ObjectId id, final Map<String, Object> fields);

        LocalAdminUser createLocalAdminUser(String adminRoleObjectId);
    }

    private static final Logger LOG = LoggerFactory.getLogger(UserImpl.class);

    private static final Map<String, Object> DEFAULT_PREFERENCES = new ImmutableMap.Builder<String, Object>()
            .put("updateUnfocussed", false)
            .put("enableSmartSearch", true)
            .build();

    public static final String COLLECTION_NAME = "users";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String FULL_NAME = "full_name";
    public static final String PERMISSIONS = "permissions";
    public static final String PREFERENCES = "preferences";
    public static final String TIMEZONE = "timezone";
    public static final String EXTERNAL_USER = "external_user";
    public static final String SESSION_TIMEOUT = "session_timeout_ms";
    public static final String STARTPAGE = "startpage";
    public static final String ROLES = "roles";

    public static final int MAX_USERNAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_FULL_NAME_LENGTH = 200;

    @AssistedInject
    public UserImpl(PasswordAlgorithmFactory passwordAlgorithmFactory,
                    Permissions permissions,
                    @Assisted final Map<String, Object> fields) {
        super(fields);
        this.passwordAlgorithmFactory = passwordAlgorithmFactory;
        this.permissions = permissions;
    }

    @AssistedInject
    protected UserImpl(PasswordAlgorithmFactory passwordAlgorithmFactory,
                       Permissions permissions,
                       @Assisted final ObjectId id,
                       @Assisted final Map<String, Object> fields) {
        super(id, fields);
        this.passwordAlgorithmFactory = passwordAlgorithmFactory;
        this.permissions = permissions;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(USERNAME, new LimitedStringValidator(1, MAX_USERNAME_LENGTH))
                .put(PASSWORD, new FilledStringValidator())
                .put(EMAIL, new LimitedStringValidator(1, MAX_EMAIL_LENGTH))
                .put(FULL_NAME, new LimitedOptionalStringValidator(MAX_FULL_NAME_LENGTH))
                .put(PERMISSIONS, new ListValidator())
                .put(ROLES, new ListValidator(true))
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(final String key) {
        return Collections.emptyMap();
    }

    @Override
    public String getFullName() {
        return String.valueOf(fields.get(FULL_NAME));
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
        return nullToEmpty((String) fields.get(EMAIL));
    }

    @Override
    public void setEmail(final String email) {
        fields.put(EMAIL, email);
    }

    @Override
    public List<String> getPermissions() {
        final Set<String> permissionSet = new HashSet<>(this.permissions.userSelfEditPermissions(getName()));
        @SuppressWarnings("unchecked")
        final List<String> permissions = (List<String>) fields.get(PERMISSIONS);
        if (permissions != null) {
            permissionSet.addAll(permissions);
        }
        return new ArrayList<>(permissionSet);
    }

    @Override
    public void setPermissions(final List<String> permissions) {
        final List<String> perms = Lists.newArrayList(permissions);
        // Do not store the dynamic user self edit permissions
        perms.removeAll(this.permissions.userSelfEditPermissions(getName()));
        fields.put(PERMISSIONS, perms);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPreferences() {
        final Map<String, Object> preferences = (Map<String, Object>) fields.get(PREFERENCES);

        return preferences == null || preferences.isEmpty() ? DEFAULT_PREFERENCES : preferences;
    }

    @Override
    public void setPreferences(final Map<String, Object> preferences) {
        fields.put(PREFERENCES, preferences);
    }

    @Override
    public Startpage getStartpage() {
        if (fields.containsKey(STARTPAGE)) {
            @SuppressWarnings("unchecked")
            final Map<String, String> obj = (Map<String, String>) fields.get(STARTPAGE);
            final String type = obj.get("type");
            final String id = obj.get("id");

            if (type != null && id != null) {
                return Startpage.create(type, id);
            }
        }

        return null;
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
    public void setPassword(final String password) {
        if (password == null || "".equals(password)) {
            // If no password is given, we leave the hashed password empty and we fail during validation.
            setHashedPassword("");
        } else {
            final String newPassword = passwordAlgorithmFactory.defaultPasswordAlgorithm().hash(password);
            setHashedPassword(newPassword);
        }
    }

    @Override
    public boolean isUserPassword(final String password) {
        final PasswordAlgorithm passwordAlgorithm = passwordAlgorithmFactory.forPassword(getHashedPassword());
        if (passwordAlgorithm == null) {
            return false;
        }

        return passwordAlgorithm.matches(getHashedPassword(), password);
    }

    @Override
    public DateTimeZone getTimeZone() {
        final Object o = fields.get(TIMEZONE);
        try {
            if (o != null) {
                return DateTimeZone.forID(o.toString());
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid timezone \"{}\" saved for user \"{}\"", o, getName());
        }
        return null;
    }

    @Override
    public void setTimeZone(final String timeZone) {
        DateTimeZone dateTimeZone = null;
        if (timeZone != null) {
            try {
                dateTimeZone = DateTimeZone.forID(timeZone);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid timezone \"{}\", falling back to UTC.", timeZone);
            }
        }
        setTimeZone(dateTimeZone);
    }

    @Override
    public void setTimeZone(final DateTimeZone timeZone) {
        fields.put(TIMEZONE, timeZone == null ? null : timeZone.getID());
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

    @Nonnull
    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getRoleIds() {
        final List<ObjectId> roles = firstNonNull((List<ObjectId>) fields.get(ROLES), Collections.<ObjectId>emptyList());
        return new HashSet<>(Collections2.transform(roles, new ObjectIdStringFunction()));
    }

    @Override
    public void setRoleIds(Set<String> roles) {
        fields.put(ROLES, new ArrayList<>(Collections2.transform(roles, new StringObjectIdFunction())));
    }

    @Override
    public void setStartpage(final String type, final String id) {
        final Startpage nextStartpage = type != null && id != null ? Startpage.create(type, id) : null;
        this.setStartpage(nextStartpage);
    }

    @Override
    public void setStartpage(Startpage startpage) {
        final HashMap<String, String> startpageMap = new HashMap<>();
        if (startpage != null) {
            startpageMap.put("type", startpage.type());
            startpageMap.put("id", startpage.id());
        }
        this.fields.put(STARTPAGE, startpageMap);
    }

    public static class LocalAdminUser extends UserImpl {
        private final Configuration configuration;
        private final Set<String> roles;

        @AssistedInject
        LocalAdminUser(PasswordAlgorithmFactory passwordAlgorithmFactory,
                       Configuration configuration,
                       @Assisted String adminRoleObjectId) {
            super(passwordAlgorithmFactory, null, null, Collections.<String, Object>emptyMap());
            this.configuration = configuration;
            this.roles = ImmutableSet.of(adminRoleObjectId);
        }

        @Override
        public String getId() {
            return "local:admin";
        }

        @Override
        public String getFullName() {
            return "Administrator";
        }

        @Override
        public String getEmail() {
            return configuration.getRootEmail();
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
            return DEFAULT_PREFERENCES;
        }

        @Override
        public long getSessionTimeoutMs() {
            return TimeUnit.HOURS.toMillis(8);
        }

        @Override
        public DateTimeZone getTimeZone() {
            return configuration.getRootTimeZone();
        }

        @Override
        public boolean isLocalAdmin() {
            return true;
        }

        @Nonnull
        @Override
        public Set<String> getRoleIds() {
            return roles;
        }

        @Override
        public void setRoleIds(Set<String> roles) {
        }
    }

}
