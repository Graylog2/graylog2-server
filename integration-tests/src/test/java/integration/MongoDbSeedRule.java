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
