package org.graylog2.users;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.users.Role;

import javax.validation.ConstraintViolation;
import java.util.Set;

public interface RoleService {
    Role loadById(String roleId) throws NotFoundException;

    Role load(String roleName) throws NotFoundException;

    Set<Role> loadAll() throws NotFoundException;

    Role save(Role role) throws ValidationException;

    Set<ConstraintViolation<Role>> validate(Role role);

    int delete(String roleName);
}
