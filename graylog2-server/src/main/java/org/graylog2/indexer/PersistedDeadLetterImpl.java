/**
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
 */
package org.graylog2.indexer;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@CollectionName("dead_letters")
public class PersistedDeadLetterImpl extends PersistedImpl implements PersistedDeadLetter {

    public PersistedDeadLetterImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected PersistedDeadLetterImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Object> asMap() {
        return new HashMap<String, Object>() {{
            put("timestamp", Tools.getISO8601String((DateTime) fields.get("timestamp")));
            put("letter_id", fields.get("letter_id"));
            put("message", fields.get("message"));
        }};
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}
