package org.graylog2.database;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.validators.Validator;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class PersistedImplTest {
    class PersistedImplSUT extends PersistedImpl {
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
    public void testGetObjectId() throws Exception {

    }

    @Test
    public void testGetId() throws Exception {

    }

    @Test
    public void testGetFields() throws Exception {

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
