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

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.users.Role;

import javax.validation.ConstraintViolation;
import java.util.Map;
import java.util.Set;

public interface RoleService {
    Role loadById(String roleId) throws NotFoundException;

    Role load(String roleName) throws NotFoundException;

    boolean exists(String roleName);

    Set<Role> loadAll();

    Map<String, Role> loadAllIdMap() throws NotFoundException;

    Map<String, Role> findIdMap(Set<String> roleIds) throws NotFoundException;

    Map<String, Role> loadAllLowercaseNameMap() throws NotFoundException;

    Role save(Role role) throws ValidationException;

    Set<ConstraintViolation<Role>> validate(Role role);

    /**
     * Deletes the (case insensitively) named role, unless it is read only.
     * @param roleName role name to delete, case insensitive
     * @return the number of deleted roles
     */
    int delete(String roleName);

    String getAdminRoleObjectId();

    String getReaderRoleObjectId();
}
