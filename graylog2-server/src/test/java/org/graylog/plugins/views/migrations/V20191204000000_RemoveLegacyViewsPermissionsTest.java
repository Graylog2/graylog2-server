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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class V20191204000000_RemoveLegacyViewsPermissionsTest {

    @ClassRule
    public static MongoDBInstance mongo = MongoDBInstance.createForClass();

    private MongoCollection<Document> rolesCollection;
    private MongoCollection<Document> usersCollection;

    private V20191204000000_RemoveLegacyViewsPermissions migration;

    @Before
    public void setUp() throws Exception {
        MongoDatabase mongoDatabase = mongo.mongoConnection().getMongoDatabase();
        mongoDatabase.drop();
        rolesCollection = mongoDatabase.getCollection("roles");
        usersCollection = mongoDatabase.getCollection("users");
        migration = new V20191204000000_RemoveLegacyViewsPermissions(mongo.mongoConnection());
    }

    @Test
    public void doesntFailIfOldPermissionsNotPresent() {
        Document role = insertRole("Dancing Monkey");
        Document user = insertUserWithRoles(role);

        migration.upgrade();

        assertThat(rolesCollection.find()).containsOnly(role);
        assertThat(usersCollection.find()).containsOnly(user);
    }

    @Test
    public void removesRoleFromRolesCollection() {
        insertRole("Views User");
        Document otherRole = insertRole("Some Other Role");

        migration.upgrade();

        assertThat(rolesCollection.find()).containsOnly(otherRole);
    }

    @Test
    public void removesReferenceToRoleFromUsers() {
        Document viewsUserRole = insertRole("Views User");
        Document otherRole = insertRole("Some Other Role");

        insertUserWithRoles(otherRole);
        Document userBefore = insertUserWithRoles(viewsUserRole, otherRole);

        migration.upgrade();

        Document userAfter = usersCollection.find(new Document().append("_id", userBefore.get("_id"))).first();

        assertThat(getList(userAfter, "roles")).containsOnly(otherRole.getObjectId("_id"));
    }
    
    // convenience method to avoid casting and suppressing warnings in assertions
    private <T> List<T> getList(Document d, String key) {
        //noinspection unchecked
        return (List<T>) d.get(key, List.class);
    }

    private Document insertUserWithRoles(Document... roles) {
        List<Object> roleIds = Arrays.stream(roles).map(r -> r.get("_id")).collect(toList());
        Document user = new Document().append("roles", roleIds);

        usersCollection.insertOne(user);
        return user;
    }

    private Document insertRole(String name) {
        return insertRoleWithPermissions(name, ImmutableSet.of());
    }

    private Document insertRoleWithPermissions(String name, Set<String> permissions) {
        Document role = new Document().append("name", name).append("permissions", new ArrayList<>(permissions));
        rolesCollection.insertOne(role);
        return role;
    }
}
