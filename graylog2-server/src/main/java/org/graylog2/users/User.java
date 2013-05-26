/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */
package org.graylog2.users;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.Persistable;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class User extends Persisted implements Persistable {
   
    private static final Logger LOG = LoggerFactory.getLogger(User.class);

    private static final String COLLECTION = "users";

    public User(Map<String, Object> fields, Core core) {
        super(COLLECTION, core, fields, validations());
    }

    protected User(ObjectId id, Map<String, Object> fields, Core core) {
        super(COLLECTION, core, id, fields, validations());
    }

    public static boolean exists(String username, String passwordHash, Core core) {
        DBObject query = new BasicDBObject();
        query.put("username", username);
        query.put("password", passwordHash);

        List<DBObject> result = query(query, core, COLLECTION);

        if (result == null)     { return false; }
        if (result.size() == 0) { return false; }

        if (result.size() > 1) {
            throw new RuntimeException("There was more than one matching user. This should never happen.");
        }

        String dbUsername = (String) result.get(0).get("username");
        String dbPasswordHash = (String) result.get(0).get("password");

        if (dbUsername != null && dbPasswordHash != null) {
            return dbUsername.equals(username) && dbPasswordHash.equals(passwordHash);
        }

        return false;
    }

    public static String saltPass(String password, String salt) {
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("No password given.");
        }

        if (salt == null || salt.isEmpty()) {
            throw new RuntimeException("No salt given.");
        }

        return password + salt;
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    private static Map<String, Validator> validations() {
        return new HashMap<String, Validator>() {{
            put("username", new FilledStringValidator());
            put("password", new FilledStringValidator());
            put("full_name", new FilledStringValidator());
        }};
    }
}
