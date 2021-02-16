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
package org.graylog2.plugin.database.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.shiro.authz.Permission;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.rest.models.users.requests.Startpage;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface User extends Persisted {
    boolean isReadOnly();

    @Nullable
    String getAuthServiceId();

    @Nullable
    String getAuthServiceUid();

    String getFirstName();

    String getLastName();

    String getFullName();

    String getName();

    void setName(String username);

    /**
     * Returns the email address of the user.
     *
     * Depending on how the user has been created, it is possible that the returned string contains multiple email
     * addresses separated by a "," character. (i.e. LDAP users)
     *
     * @return the email address
     */
    String getEmail();

    List<String> getPermissions();

    Set<Permission> getObjectPermissions();

    Map<String, Object> getPreferences();

    Startpage getStartpage();

    long getSessionTimeoutMs();

    void setSessionTimeoutMs(long timeoutValue);

    void setPermissions(List<String> permissions);

    void setPreferences(Map<String, Object> preferences);

    void setAuthServiceId(@Nullable String authServiceId);

    void setAuthServiceUid(@Nullable String authServiceUid);

    void setEmail(String email);

    void setFullName(String firstName, String lastName);

    void setFullName(String fullname);

    String getHashedPassword();

    void setPassword(String password);

    boolean isUserPassword(String password);

    DateTimeZone getTimeZone();

    void setTimeZone(DateTimeZone timeZone);

    void setTimeZone(String timeZone);

    boolean isExternalUser();

    void setExternal(boolean external);

    void setStartpage(String type, String id);

    void setStartpage(Startpage startpage);

    boolean isLocalAdmin();

    @Nonnull
    Set<String> getRoleIds();

    void setRoleIds(Set<String> roles);

    void setAccountStatus(AccountStatus status);

    AccountStatus getAccountStatus();

    enum AccountStatus {
        @JsonProperty("enabled")
        ENABLED,
        @JsonProperty("disabled")
        DISABLED,
        @JsonProperty("deleted")
        DELETED
    }
}
