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

import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import static com.lordofthejars.nosqlunit.mongodb.MongoDbRule.MongoDbRuleBuilder.newMongoDbRule;

public class MongoConnectionRule implements MethodRule {
    private final String dbName;
    private final MongoDbRule mongoDbRule;

    public MongoConnectionRule(String dbName) {
        this.dbName = dbName;
        this.mongoDbRule = newMongoDbRule().defaultEmbeddedMongoDb(dbName);
    }

    public static MongoConnectionRule build(String dbName) {
        return new MongoConnectionRule(dbName);
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return mongoDbRule.apply(base, method, target);
    }

    public MongoConnection getMongoConnection() {
        return new MongoConnectionForTests(mongoDbRule.getDatabaseOperation().connectionManager(), dbName);
    }
}
