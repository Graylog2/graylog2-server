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

import integration.util.graylog.ServerHelper;
import integration.util.mongodb.MongodbSeed;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import static com.google.common.base.Strings.emptyToNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class MongoDbSeedRule implements MethodRule {
    private static final Logger log = LoggerFactory.getLogger(MongoDbSeedRule.class);
    private static final String CK_RESOURCEPREFIX = "integration/seeds/mongodb/";
    private static final String CK_JSONSUFFIX = ".json";

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        final MongoDbSeed annotation = firstNonNull(method.getAnnotation(MongoDbSeed.class),
                method.getDeclaringClass().getAnnotation(MongoDbSeed.class));
        if (annotation != null) {
            final String databaseName = firstNonNull(emptyToNull(annotation.database()), IntegrationTestsConfig.getMongodbDatabase());
            final MongodbSeed mongodbSeed;
            try {
                mongodbSeed = new MongodbSeed(databaseName);
            } catch (UnknownHostException e) {
                final String msg = "Unable to seed database: ";
                log.error(msg, e);
                return new IgnoreStatement(msg + e.toString());
            }
            final ServerHelper graylogController = new ServerHelper();
            final String nodeId;
            try {
                nodeId = graylogController.getNodeId();
            } catch (MalformedURLException | URISyntaxException e) {
                final String msg = "Unable to determine Graylog node id for seeding: ";
                log.error(msg, e);
                return new IgnoreStatement(msg + e.toString());
            }

            if (annotation.locations().length > 0) {
                for (String location : annotation.locations()) {
                    final URL seedUrl = findFirstInSearchPath(location, method.getDeclaringClass());
                    if (seedUrl == null) {
                        throw new RuntimeException("Unable to find seed data " + location + " for " + method.toString());
                    }

                    try {
                        log.debug("Using seed data from {}", seedUrl);
                        mongodbSeed.loadDataset(seedUrl, nodeId);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException("Unable to read seed data: ", e);
                    }
                }
            } else {
                final URL seedUrl = findFirstInSearchPath(method.getName(), method.getDeclaringClass());
                try {
                    if (seedUrl != null) {
                        log.debug("Using seed data from {}", seedUrl);
                        mongodbSeed.loadDataset(seedUrl, nodeId);
                    }
                } catch (IOException | URISyntaxException e) {
                    log.debug("MongoDB seed annotation present, but neither explicit location passed, nor inferenced location available, just cleaning databse. Exception was: ", e);
                }
            }
        }
        return base;
    }

    private URL findFirstInSearchPath(String suffix, Class<?> testClass) {
        return firstNonNull(
                firstNonNull(getClassSpecificResource(testClass, suffix), getClassSpecificResource(testClass, suffix + CK_JSONSUFFIX)),
                firstNonNull(getGlobalResource(suffix), getGlobalResource(suffix + CK_JSONSUFFIX))
        );
    }

    private URL getGlobalResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(CK_RESOURCEPREFIX + name);
    }

    private URL getClassSpecificResource(Class klazz, String name) {
        return klazz.getResource(klazz.getSimpleName() + "/" + name);
    }
}
