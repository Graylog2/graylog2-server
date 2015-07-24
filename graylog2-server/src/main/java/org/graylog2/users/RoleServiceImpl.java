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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.users.Role;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;

public class RoleServiceImpl implements RoleService {

    private static final String ROLES = "roles";
    private static final String NAME_LOWER = "name_lower";

    private final JacksonDBCollection<RoleImpl, ObjectId> dbCollection;
    private final Validator validator;

    @Inject
    protected RoleServiceImpl(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper,
                              Validator validator) {
        this.validator = validator;

        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(ROLES),
                RoleImpl.class,
                ObjectId.class,
                mapper.get());
        // lower case role names are unique, this allows arbitrary naming, but still uses an index
        dbCollection.createIndex(new BasicDBObject(NAME_LOWER, 1), new BasicDBObject("unique", true));
    }

    @Override
    public Role loadById(String roleId) throws NotFoundException {
        final Role role = dbCollection.findOneById(new ObjectId(roleId));
        if (role == null) {
            throw new NotFoundException("No role found with id " + roleId);
        }
        return role;
    }

    @Override
    public RoleImpl load(String roleName) throws NotFoundException {
        final RoleImpl role = dbCollection.findOne(DBQuery.is(NAME_LOWER, roleName.toLowerCase()));

        if (role == null) {
            throw new NotFoundException("No role found with name " + roleName);
        }
        return role;
    }

    @Override
    public Set<Role> loadAll() throws NotFoundException {
        final DBCursor<RoleImpl> rolesCursor = dbCollection.find();
        Set<Role> roles = Sets.newHashSet();
        if (rolesCursor.hasNext()) {
            Iterators.addAll(roles, rolesCursor);
        }
        return roles;
    }

    @Override
    public Map<String, Role> loadAllIdMap() throws NotFoundException {
        final Set<Role> roles = loadAll();
        return Maps.uniqueIndex(roles, new Function<Role, String>() {
            @Nullable
            @Override
            public String apply(Role input) {
                return input.getId();
            }
        });
    }

    @Override
    public RoleImpl save(Role role1) throws ValidationException {
        // sucky but necessary because of graylog2-shared not knowing about mongodb :(
        if (!(role1 instanceof RoleImpl)) {
            throw new IllegalArgumentException("invalid Role implementation class");
        }
        RoleImpl role = (RoleImpl) role1;
        final Set<ConstraintViolation<Role>> violations = validate(role);
        if (!violations.isEmpty()) {
            throw new ValidationException("Validation failed.", violations.toString());
        }
        final WriteResult<RoleImpl, ObjectId> writeResult = dbCollection.save(role);
        return writeResult.getSavedObject();
    }

    @Override
    public Set<ConstraintViolation<Role>> validate(Role role) {
        return validator.validate(role);
    }

    @Override
    public int delete(String roleName) {
        return dbCollection.remove(DBQuery.is(NAME_LOWER, roleName.toLowerCase())).getN();
    }


}
