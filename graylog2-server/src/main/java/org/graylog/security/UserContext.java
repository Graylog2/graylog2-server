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
package org.graylog.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class UserContext {
    private final UserService userService;
    private final String userId;
    private final Subject subject;

    public static class Factory {
        private final UserService userService;

        @Inject
        public Factory(UserService userService) {
            this.userService = userService;
        }

        /**
         * Create a UserContext from the currently accessible Shiro Subject available to the calling code depending on runtime environment.
         * This should only be called from within an existing Shiro context.
         * If a UserContext is needed from an environment where there is no existing context,
         * the code can be run using: {@link UserContext#runAs(String username, Callable)}
         *
         * @return a user context reflecting the currently executing user.
         * @throws UserContextMissingException
         */
        public UserContext create() throws UserContextMissingException {
            try {
                final Subject subject = SecurityUtils.getSubject();
                final Object userId = subject.getPrincipal();
                if (!(userId instanceof String)) {
                    throw new UserContextMissingException("Unknown SecurityContext class <" + userId + ">, cannot continue.");
                }
                return new UserContext((String) userId, subject, userService);
            } catch (IllegalStateException | UnavailableSecurityManagerException e) {
                throw new UserContextMissingException("Cannot retrieve current subject, SecurityContext isn't set.");
            }
        }

        public UserContext create(Subject subject) {
            return new UserContext((String) subject.getPrincipal(), subject, userService);
        }
    }

    /**
     * Build a temporary Shiro Subject and run the callable within that context
     * @param username  The username of the subject
     * @param callable  The callable to be executed
     * @param <T>       The return type of the callable.
     * @return          whatever the callable returns.
     */
    public static <T> T runAs(String username, Callable<T> callable) {
        final Subject subject = new Subject.Builder()
                .principals(new SimplePrincipalCollection(username, "runAs-context"))
                .authenticated(true)
                .sessionCreationEnabled(false)
                .buildSubject();

        return subject.execute(callable);
    }

    /**
     * Build a temporary Shiro Subject and run the callable within that context
     * @param username  The username of the subject
     * @param runnable  The runnable to be executed
     */
    public static void runAs(String username, Runnable runnable) {
        final Subject subject = new Subject.Builder()
                .principals(new SimplePrincipalCollection(username, "runAs-context"))
                .authenticated(true)
                .sessionCreationEnabled(false)
                .buildSubject();

        subject.execute(runnable);
    }

    public UserContext(String userId, Subject subject, UserService userService) {
        this.userId = userId;
        this.subject = subject;
        this.userService = userService;
    }

    public String getUserId() {
        return userId;
    }

    public User getUser() {
        return Optional.ofNullable(userService.loadById(userId)).orElseThrow(() -> new IllegalStateException("Cannot load user <" + userId + "> from db"));
    }

    protected boolean isOwner(GRN entity) {
        return subject.isPermitted(GRNPermission.create(RestPermissions.ENTITY_OWN, entity));
    }

    /**
     * Checks if the user is permitted to do everything
     * @return The check result
     */
    public boolean hasAllPermission() {
        return subject.isPermitted(new AllPermission());
    }

    public boolean isPermitted(String permission, GRN target) {
        return isPermitted(permission, target.entity());
    }

    public boolean isPermitted(String permission, String id) {
        checkArgument(isNotBlank(permission), "permission cannot be null or empty");
        checkArgument(isNotBlank(id), "id cannot be null or empty");
        return subject.isPermitted(permission + ":" + id);
    }

    public boolean isPermitted(String permission) {
        checkArgument(isNotBlank(permission), "permission cannot be null or empty");
        return subject.isPermitted(permission);
    }
}

