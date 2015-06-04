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
