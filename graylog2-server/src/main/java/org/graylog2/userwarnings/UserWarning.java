/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.userwarnings;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class UserWarning extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(UserWarning.class);

    public static final String COLLECTION = "user_warnings";

    public enum Type {
        DEFLECTOR_EXISTS_AS_INDEX
    }

    public enum Severity {
        NORMAL, URGENT
    }

    protected UserWarning(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public static void issue(Core core, Type type, Severity severity) {
        // Write only if there is no such warning yet.
        if (!first(core, type)) {
            return;
        }

        Map<String, Object> fields = Maps.newHashMap();
        fields.put("type", type.toString().toLowerCase());
        fields.put("severity", severity.toString().toLowerCase());
        fields.put("timestamp", Tools.iso8601());

        UserWarning w = new UserWarning(core, fields);

        try {
            w.save();
        } catch(ValidationException e) {
            // We have no validations, but just in case somebody adds some...
            LOG.error("Validating user warning failed.", e);
        }
    }

    public static boolean first(Core core, Type type) {
        return (UserWarning.findOne(new BasicDBObject("type", type.toString().toLowerCase()), core, COLLECTION) != null);
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

}
