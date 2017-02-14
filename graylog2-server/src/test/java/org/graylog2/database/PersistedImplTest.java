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
package org.graylog2.database;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.validators.Validator;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PersistedImplTest {
    private static class PersistedImplSUT extends PersistedImpl {
        PersistedImplSUT(Map<String, Object> fields) {
            super(fields);
        }

        PersistedImplSUT(ObjectId id, Map<String, Object> fields) {
            super(id, fields);
        }

        @Override
        public Map<String, Validator> getValidations() {
            return null;
        }

        @Override
        public Map<String, Validator> getEmbeddedValidations(String key) {
            return null;
        }
    }

    @Test
    public void testConstructorWithFieldsOnly() throws Exception {
        Map<String, Object> fields = Maps.newHashMap();
        Persisted persisted = new PersistedImplSUT(fields);
        assertNotNull(persisted);
        assertNotNull(persisted.getId());
        assertFalse(persisted.getId().isEmpty());
    }

    @Test
    public void testConstructorWithFieldsAndId() throws Exception {
        Map<String, Object> fields = Maps.newHashMap();
        ObjectId id = new ObjectId();
        Persisted persisted = new PersistedImplSUT(id, fields);
        assertNotNull(persisted);
        assertNotNull(persisted.getId());
        assertFalse(persisted.getId().isEmpty());
        assertEquals(id.toString(), persisted.getId());
    }

    @Test
    public void testEqualityForSameRecord() throws Exception {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("foo", "bar");
        fields.put("bar", 42);

        ObjectId id = new ObjectId();

        Persisted persisted1 = new PersistedImplSUT(id, fields);
        Persisted persisted2 = new PersistedImplSUT(id, fields);

        assertEquals(persisted1, persisted2);
    }
}
