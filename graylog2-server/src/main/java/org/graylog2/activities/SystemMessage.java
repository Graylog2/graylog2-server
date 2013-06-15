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
package org.graylog2.activities;

import com.beust.jcommander.internal.Maps;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persistable;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemMessage extends Persisted implements Persistable {

    private static final String COLLECTION = "system_messages";

    public SystemMessage(Map<String, Object> fields, Core core) {
        super(COLLECTION, core, fields, validations());
    }

    protected SystemMessage(ObjectId id, Map<String, Object> fields, Core core) {
        super(COLLECTION, core, id, fields, validations());
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    private static Map<String, Validator> validations() {
        // We don't have any, this is used internally only.
        return Maps.newHashMap();
    }
}