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

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

class MongoDBExtensionTest {
    @Nested
    class ProgrammaticInstanceRegistration {
        @SuppressWarnings("unused")
        @RegisterExtension
        MongoDBExtension mongodbExtension = MongoDBExtension.createWithDefaultVersion();

        @Test
        @MongoDBFixtures("MongoDBExtensionTest-1.json")
        void withFixtureImport(MongoDBTestService mongodb) {
            assertCollection(mongodb, "test_1", eq("hello", "world"), "54e3deadbeefdeadbeef0000");
        }

        @Test
        void withoutFixtures(MongoDBTestService mongodb) {
            assertThat(mongodb.mongoDatabase().listCollections()).isEmpty();
        }
    }

    @Nested
    @ExtendWith(MongoDBExtension.class)
    class DeclarativeRegistration {
        private MongoCollection<Document> collection;

        @BeforeEach
        void setUp(MongoDBTestService mongodb) {
            collection = mongodb.mongoCollection("test_2");
        }

        @Test
        @MongoDBFixtures("MongoDBExtensionTest-1.json")
        void withSetUpInject() {
            assertCollection(collection, eq("field_a", "content1"), "54e3deadbeefdeadbeef0001");
        }

        @Test
        @MongoDBFixtures("MongoDBExtensionTest-1.json")
        void withMethodInject(MongoDBTestService mongodb) {
            assertCollection(mongodb, "test_2", eq("field_a", "content1"), "54e3deadbeefdeadbeef0001");
        }

        @Test
        void withoutFixtures(MongoDBTestService mongodb) {
            assertThat(mongodb.mongoDatabase().listCollections()).isEmpty();
        }
    }

    @Nested
    @ExtendWith(MongoDBExtension.class)
    @MongoDBFixtures("MongoDBExtensionTest-2.json")
    class ClassFixtures {
        @Test
        void withoutMethodAnnotation(MongoDBTestService mongodb) {
            // Is using the class annotation
            assertCollection(mongodb, "test_3", eq("key", "value1"), "54e3deadbeefdeadbeef0000");
            assertEmptyCollection(mongodb, "test_1");
        }

        @Test
        @MongoDBFixtures("MongoDBExtensionTest-1.json")
        void withMethodAnnotation(MongoDBTestService mongodb) {
            // Is ONLY using the method annotation and doesn't load the fixture from the class annotation
            assertEmptyCollection(mongodb, "test_3");
            assertCollection(mongodb, "test_2", eq("field_a", "content1"), "54e3deadbeefdeadbeef0001");
        }
    }

    private void assertEmptyCollection(MongoDBTestService mongodb, String collectionName) {
        assertThat(mongodb.mongoCollection(collectionName).countDocuments()).isEqualTo(0);
    }

    private void assertCollection(MongoDBTestService mongodb, String collectionName, Bson query, String expectedId) {
        assertCollection(mongodb.mongoCollection(collectionName), query, expectedId);
    }

    private void assertCollection(MongoCollection<Document> collection, Bson query, String expectedId) {
        assertThat(collection.find(query).first())
                .satisfies(document -> {
                    assertThat(document).isNotNull();
                    assertThat(document.get("_id").toString()).isEqualTo(expectedId);
                });
    }
}
