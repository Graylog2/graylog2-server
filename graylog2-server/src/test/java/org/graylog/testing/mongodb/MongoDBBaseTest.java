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

import org.graylog2.database.MongoConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

/**
 * This class can be used as base class for MongoDB integration tests.
 * <p>
 * It starts a MongoDB instance for each implementing test class.
 * <p>
 * Use {@link MongoDBFixtures} annotations for your test methods to load fixture data into the database.
 */
public abstract class MongoDBBaseTest {
    @ClassRule
    public static final MongoDBInstance MONGODB = MongoDBInstance.create();

    @Rule
    public final MongoDBFixturesWatcher mongoDBFixturesWatcher = new MongoDBFixturesWatcher();

    private final MongoDBFixtureImporter fixtureImporter = new MongoDBFixtureImporter();

    @Before
    public void mongoDBBaseTestBefore() {
        fixtureImporter.importResources(mongoDBFixturesWatcher.fixtureResources(getClass()), MONGODB.mongoConnection().getMongoDatabase());
    }

    @After
    public void mongoDBBaseTestAfter() {
        MONGODB.dropDatabase();
    }

    /**
     * Returns an established connection to the started MongoDB instance.
     *
     * @return the established connection object
     */
    public MongoConnection mongoConnection() {
        return MONGODB.mongoConnection();
    }
}
