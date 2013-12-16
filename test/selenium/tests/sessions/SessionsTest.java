/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package selenium.tests.sessions;

import org.fluentlenium.core.annotation.Page;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import selenium.pages.DashboardPage;
import selenium.pages.LoginPage;

import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SessionsTest extends BaseSeleniumTest {
    private static final Logger log = LoggerFactory.getLogger(SessionsTest.class);

    @Page
    public LoginPage loginPage;

    @BeforeClass
    public static void setup() {
        skipOnTravis();
    }

    @Test
    public void login() {
        running(testServer(WEB_PORT, getApp()), new Runnable() {
            @Override
            public void run() {
                loginPage.go();
                final DashboardPage dashboardPage = loginPage.loginAs("admin", "admin");
                assertThat(dashboardPage).isAt();
            }
        });
    }

    @Test
    public void loginErrorNoUser() {
        running(testServer(WEB_PORT, getApp()), new Runnable() {
            @Override
            public void run() {
                loginPage.go();
                final LoginPage loginPage1 = loginPage.loginWithError("admin", "");
                assertThat(loginPage1).isAt();
            }
        });
    }
}
