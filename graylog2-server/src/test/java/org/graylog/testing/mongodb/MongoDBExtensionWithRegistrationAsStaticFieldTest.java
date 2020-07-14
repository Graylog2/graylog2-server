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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing programmatic extension registration with static fields has been moved to this separate file because
 * we cannot use static fields on nested classes in {@link MongoDBExtensionTest}.
 */
class MongoDBExtensionWithRegistrationAsStaticFieldTest {
    @SuppressWarnings("unused")
    @RegisterExtension
    static MongoDBExtension mongodbExtension = MongoDBExtension.createWithDefaultVersion();

    // We use this static string to verify that registering the MongoDBExtension as a static field will reuse the
    // same database instance for all tests.
    static String instanceId = null;

    @Nested
    // Required to make the instance ID check work
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UsingSameInstanceForAllTests {
        @Test
        @Order(1)
        void recordInstanceId(MongoDBTestService mongodb) {
            // Just set the MongoDB instance ID so we can check them in the next test
            instanceId = mongodb.instanceId();
        }

        @Test
        @Order(2)
        void checkThatSameInstanceIdIsUsed(MongoDBTestService mongodb) {
            assertThat(instanceId)
                    .withFailMessage("All test methods should use the same MongoDB instance, but we registered more than one")
                    .isEqualTo(mongodb.instanceId());
        }
    }

    @Test
    @MongoDBFixtures("MongoDBExtensionTest-1.json")
    void withFixtures(MongoDBTestService mongodb) {
        assertThat(mongodb.mongoCollection("test_1").find(eq("hello", "world")).first())
                .satisfies(document -> {
                    assertThat(document).isNotNull();
                    assertThat(document.get("_id").toString()).isEqualTo("54e3deadbeefdeadbeef0000");
                });
    }

    @Test
    void withoutFixtures(MongoDBTestService mongodb) {
        assertThat(mongodb.mongoCollection("test_1").countDocuments()).isEqualTo(0);
        assertThat(mongodb.mongoCollection("test_2").countDocuments()).isEqualTo(0);
    }
}
