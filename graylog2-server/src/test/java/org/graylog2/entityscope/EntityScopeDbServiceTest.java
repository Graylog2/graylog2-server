package org.graylog2.entityscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.utilities.StringUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityScopeDbServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private EntityScopeDbService dbService;

    @BeforeAll
    void init() {

        mongodb.start();
        mongodb.importFixture("entity-scope-test-fixture.json", EntityScope.class);

        final MongoConnection conn = mongodb.mongoConnection();
        dbService = new EntityScopeDbService(conn, new MongoJackObjectMapperProvider(new ObjectMapper()));
    }

    @AfterAll
    void cleanup() {
        mongodb.close();
    }

    @Test
    void testIsModifiableByTitle() {

        boolean isModifiable = dbService.isModifiable("modifiable_scope");
        assertTrue(isModifiable);
    }

    @Test
    void testIsModifiableById() {

        boolean isModifiable = dbService.isModifiable("62c5929ccb6a3f40a314c8a7");
        assertTrue(isModifiable);
    }

    @Test
    void testIsModifiableNotFoundByIdOrTitle() {

        final String scope = "invalid_scope_argument";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dbService.isModifiable(scope));
        String expectedError = StringUtils.format("Entity Scope '%s' not found", scope);
        assertEquals(expectedError, exception.getMessage());

    }
}
