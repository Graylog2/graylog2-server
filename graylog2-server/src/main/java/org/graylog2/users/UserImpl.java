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

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.authz.permission.WildcardPermission;
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
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

@CollectionName(UserImpl.COLLECTION_NAME)
public class UserImpl extends PersistedImpl implements User {

    public static final String FULL_NAME_FORMAT = "%s %s";

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
    public static final String AUTH_SERVICE_ID = "auth_service_id";
    public static final String AUTH_SERVICE_UID = "auth_service_uid";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String FULL_NAME = "full_name";
    public static final String PERMISSIONS = "permissions";
    public static final String PREFERENCES = "preferences";
    public static final String TIMEZONE = "timezone";
    public static final String EXTERNAL_USER = "external_user";
    public static final String SESSION_TIMEOUT = "session_timeout_ms";
    public static final String STARTPAGE = "startpage";
    public static final String ROLES = "roles";
    public static final String ACCOUNT_STATUS = "account_status";

    public static final int MAX_USERNAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_NAME_LENGTH = 200;

    public static final long DEFAULT_SESSION_TIMEOUT_MS = TimeUnit.HOURS.toMillis(8);

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
                .put(FIRST_NAME, new LimitedOptionalStringValidator(MAX_NAME_LENGTH))
                .put(LAST_NAME, new LimitedOptionalStringValidator(MAX_NAME_LENGTH))
                .put(FULL_NAME, new LimitedOptionalStringValidator(MAX_NAME_LENGTH))
                .put(PERMISSIONS, new ListValidator())
                .put(ROLES, new ListValidator(true))
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(final String key) {
        return Collections.emptyMap();
    }

    @Override
    public String getFirstName() {
        final Object firstName = fields.get(FIRST_NAME);
        return firstName != null ? String.valueOf(firstName) : null;
    }

    @Override
    public String getLastName() {
        final Object lastName = fields.get(LAST_NAME);
        return lastName != null ? String.valueOf(lastName) : null;
    }

    /**
     * @return The full user name formatted as "First Last".
     * first_name and last_name fields were added in Graylog 4.1.
     * So, they might not be present for users that existed before.
     * Fall back to full_name when they are not present.
     */
    @Override
    public String getFullName() {
        final Object firstName = fields.get(FIRST_NAME);
        final Object lastName = fields.get(LAST_NAME);
        if (firstName != null && lastName != null ) {
            return String.format(Locale.ENGLISH, FULL_NAME_FORMAT, firstName, lastName);
        }

        return String.valueOf(fields.get(FULL_NAME));
    }

    /**
     * Set the user's fullName, firstName, and lastName. The fullName is composed with the {@link #FULL_NAME_FORMAT}
     * @param firstName The user's first name.
     * @param lastName The user's last name.
     */
    @Override
    public void setFullName(final String firstName, final String lastName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(firstName), "A first_name value is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(lastName), "A last_name value is required.");
        fields.put(FIRST_NAME, firstName);
        fields.put(LAST_NAME, lastName);
        fields.put(FULL_NAME, String.format(Locale.ENGLISH, FULL_NAME_FORMAT, firstName, lastName));
    }

    /**
     * Set the user's full name. Starting in Graylog 4.1, use of this method is discouraged.
     * Prefer use of the {@link #setFullName(String, String)} method instead when possible.
     */
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
    public Set<Permission> getObjectPermissions() {
        return getPermissions().stream().map(p -> {
            if (p.equals("*")) {
                return new AllPermission();
            } else {
                return new WildcardPermission(p);
            }

        }).collect(Collectors.toSet());
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
        return DEFAULT_SESSION_TIMEOUT_MS;
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

    @Nullable
    @Override
    public String getAuthServiceId() {
        return (String) fields.get(AUTH_SERVICE_ID);
    }

    @Nullable
    @Override
    public String getAuthServiceUid() {
        return (String) fields.get(AUTH_SERVICE_UID);
    }

    @Override
    public void setAuthServiceId(@Nullable String authServiceId) {
        fields.put(AUTH_SERVICE_ID, authServiceId);
    }

    @Override
    public void setAuthServiceUid(@Nullable String authServiceUid) {
        fields.put(AUTH_SERVICE_UID, authServiceUid);
    }

    @Override
    public void setAccountStatus(AccountStatus status) {
        fields.put(ACCOUNT_STATUS, status.toString().toLowerCase(Locale.US));
    }

    @Override
    public AccountStatus getAccountStatus() {
        final String status = (String) fields.get(ACCOUNT_STATUS);
        if (status == null) {
            return AccountStatus.ENABLED;
        }
        return AccountStatus.valueOf(status.toUpperCase(Locale.US));
    }

    public static class LocalAdminUser extends UserImpl {
        public static final String LOCAL_ADMIN_ID = "local:admin";
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
            return LOCAL_ADMIN_ID;
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
        public Set<Permission> getObjectPermissions() {
            return Collections.singleton(new AllPermission());
        }

        @Override
        public Map<String, Object> getPreferences() {
            return DEFAULT_PREFERENCES;
        }

        @Override
        public long getSessionTimeoutMs() {
            return DEFAULT_SESSION_TIMEOUT_MS;
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
