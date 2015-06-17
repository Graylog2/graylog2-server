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
package integration;

import integration.util.graylog.GraylogControl;
import integration.util.mongodb.MongodbSeed;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class MongoDbSeedRule implements MethodRule {
    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        final MongoDbSeed classAnnotation = method.getAnnotation(MongoDbSeed.class);
        if (classAnnotation != null) {
            MongodbSeed mongodbSeed = new MongodbSeed();
            GraylogControl graylogController = new GraylogControl();
            String nodeId = graylogController.getNodeId();
            graylogController.stopServer();
            mongodbSeed.loadDataset(classAnnotation.location(), classAnnotation.database(), nodeId);
            graylogController.startServer();
        }
        return base;
    }
}
