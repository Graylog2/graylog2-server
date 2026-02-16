/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.UserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MongoDBExtension.class)
public class RoleRemoverTest {

    private static final String ADMIN_ROLE = "77777ef17ad37b64ee87eb57";
    private static final String FTM_MANAGER_ROLE = "77777ef17ad37b64ee87ebdd";
    private static final String TEST_ADMIN_USER_WITH_BOTH_ROLES = "test-admin-user-with-both-roles";
    private static final String TEST_USER_WITH_FTM_MANAGER_ROLE_ONLY = "test-user-with-field-type-manager-role-only";

    private RoleRemover toTest;

    private MongoCollection<Document> rolesCollection;

    private MongoCollection<Document> usersCollection;

    @BeforeEach
    public void setUp(MongoDBTestService testService) throws Exception {
        final MongoConnection mongoConnection = testService.mongoConnection();
        testService.importFixture("roles_and_users.json", RoleRemoverTest.class);
        final MongoDatabase mongoDatabase = mongoConnection.getMongoDatabase();
        rolesCollection = mongoDatabase.getCollection(RoleServiceImpl.ROLES_COLLECTION_NAME);
        usersCollection = mongoDatabase.getCollection(UserImpl.COLLECTION_NAME);
        toTest = new RoleRemover(mongoConnection);
    }

    @Test
    public void testAttemptToRemoveNonExistingRoleDoesNotHaveEffectOnExistingUsersAndRoles() {
        final Document adminUserBefore = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_ADMIN_USER_WITH_BOTH_ROLES)).first();
        final Document testUserBefore = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_USER_WITH_FTM_MANAGER_ROLE_ONLY)).first();

        toTest.removeBuiltinRole("Bayobongo!");

        //both roles remain in DB
        assertEquals(2, rolesCollection.countDocuments());

        //both users are unchanged
        final Document adminUser = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_ADMIN_USER_WITH_BOTH_ROLES)).first();
        final Document testUser = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_USER_WITH_FTM_MANAGER_ROLE_ONLY)).first();
        assertEquals(adminUserBefore, adminUser);
        assertEquals(testUserBefore, testUser);
    }

    @Test
    public void testRemovesRoleAndItsUsageInUsersCollection() {
        toTest.removeBuiltinRole("Field Type Mappings Manager");

        //only one role remain in DB
        assertEquals(1, rolesCollection.countDocuments());
        //Field Type Mappings Manager is gone
        assertNull(rolesCollection.find(Filters.eq("_id", FTM_MANAGER_ROLE)).first());

        //both users are changed, they do not reference Field Type Mappings Manager anymore
        final Document adminUser = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_ADMIN_USER_WITH_BOTH_ROLES)).first();
        List<ObjectId> roles = adminUser.getList(UserImpl.ROLES, ObjectId.class);
        assertEquals(1, roles.size());
        assertTrue(roles.contains(new ObjectId(ADMIN_ROLE)));
        assertFalse(roles.contains(new ObjectId(FTM_MANAGER_ROLE)));

        final Document testUser = usersCollection.find(Filters.eq(UserImpl.USERNAME, TEST_USER_WITH_FTM_MANAGER_ROLE_ONLY)).first();
        roles = testUser.getList(UserImpl.ROLES, ObjectId.class);
        assertTrue(roles.isEmpty());
    }
}
