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

    Set<Role> loadAll() throws NotFoundException;

    Map<String, Role> loadAllIdMap() throws NotFoundException;

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
