package integration;

import integration.util.graylog.GraylogControl;
import integration.util.mongodb.MongodbSeed;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class SeedListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult iTestResult) {
        final MongoDbSeed classAnnotation = iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(MongoDbSeed.class);
        if (classAnnotation != null) {
            MongodbSeed mongodbSeed = new MongodbSeed();
            GraylogControl graylogController = new GraylogControl();
            String nodeId = graylogController.getNodeId();
            graylogController.stopServer();
            mongodbSeed.loadDataset(classAnnotation.location(), classAnnotation.database(), nodeId);
            graylogController.startServer();
        }
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {

    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {

    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    @Override
    public void onStart(ITestContext iTestContext) {

    }

    @Override
    public void onFinish(ITestContext iTestContext) {

    }
}
