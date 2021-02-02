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
package org.graylog2.shared.users;

import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;

/**
 * User management extension for the UserService. Initially intended to be used in the UserResource for user
 * management-specific operations that require specific UserService behavior to be augmented/overridden. Also allows
 * new user management-specific operations to be added. This allows all other direct usages of UserService to
 * remain unchanged.
 */
public interface UserManagementService extends UserService {

    /**
     * Additional method allows explicit update operations to be carried out
     * (as opposed to calling .save)
     */
    String update(User model) throws ValidationException;

    void setUserStatus(User user, User.AccountStatus status) throws ValidationException;

    boolean isUserPassword(User user, String password);

    void changePassword(User user, String oldPassword, String newPassword) throws ValidationException;

    void changePassword(User user, String newPassword) throws ValidationException;
}
