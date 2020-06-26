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
package org.graylog.testing.mongodb;

import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing programmatic extension registration with static fields has been moved to this separate file because
 * we cannot use static fields on nested classes in {@link MongoDBExtensionTest}.
 */
// Required to make the instance ID check work
@TestMethodOrder(OrderAnnotation.class)
class MongoDBExtensionWithRegistrationAsStaticFieldTest {
    @SuppressWarnings("unused")
    @RegisterExtension
    static MongoDBExtension mongodbExtension = MongoDBExtension.create();

    // We use this static set to verify that registering the MongoDBExtension as a static field will reuse the
    // same database instance for all tests.
    static Set<String> instanceIds = new HashSet<>();

    @BeforeAll
    static void setUp() {
        instanceIds.clear();
    }

    @AfterAll
    static void tearDown() {
        instanceIds.clear();
    }

    @Test
    @Order(1)
    void registerInstanceId1(MongoDBTestService mongodb) {
        // Just add the MongoDB instance ID so we can check them in checkInstanceIds()
        instanceIds.add(mongodb.instanceId());
    }

    @Test
    @Order(2)
    void registerInstanceId2(MongoDBTestService mongodb) {
        // Just add the MongoDB instance ID so we can check them in checkInstanceIds()
        instanceIds.add(mongodb.instanceId());
    }

    @Test
    @Order(3)
    void checkInstanceIds() {
        assertThat(instanceIds)
                .withFailMessage("All test methods should use the same MongoDB instance, but we registered more than one")
                .hasSize(1);
    }

    @Test
    @Order(4)
    @MongoDBFixtures("MongoDBExtensionTest-1.json")
    void withFixtures(MongoDBTestService mongodb) {
        assertThat(mongodb.mongoConnection().getMongoDatabase().getCollection("test_1").find(eq("hello", "world")).first())
                .satisfies(document -> {
                    assertThat(document).isNotNull();
                    assertThat(document.get("_id").toString()).isEqualTo("54e3deadbeefdeadbeef0000");
                });
    }

    @Test
    @Order(5)
    void withoutFixtures(MongoDBTestService mongodb) {
        final MongoDatabase database = mongodb.mongoConnection().getMongoDatabase();
        assertThat(database.getCollection("test_1").countDocuments()).isEqualTo(0);
        assertThat(database.getCollection("test_2").countDocuments()).isEqualTo(0);
    }
}
