/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2;

import org.slf4j.MDC;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;

@Listeners(Graylog2BaseTest.class)
public class Graylog2BaseTest implements ITestListener {

    public Graylog2BaseTest() {

    }

    @Override
    public void onTestStart(ITestResult result) {
        MDC.put("testngname", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        MDC.remove("testngname");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        MDC.remove("testngname");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        MDC.remove("testngname");
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        MDC.remove("testngname");
    }

    @Override
    public void onStart(ITestContext context) {
        MDC.clear();
    }

    @Override
    public void onFinish(ITestContext context) {
        MDC.clear();
    }
}
