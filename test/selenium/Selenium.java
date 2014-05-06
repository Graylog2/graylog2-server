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
package selenium;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import play.test.TestBrowser;
import selenium.serverstub.ServerStub;

import java.net.MalformedURLException;
import java.net.URL;

import static play.mvc.Http.HeaderNames.SERVER;
import static play.test.Helpers.testBrowser;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Selenium {

    public static int SERVER_STUB_PORT = 9005;
    public static final int WEB_PORT = 9999;

    protected static ServerStub serverStub;

    protected WebDriver driver;
    protected TestBrowser browser;

    protected Selenium() { }

    @BeforeClass
    public static void setUpServerStub() {
        // The port allocation sucks. It is done this way because Grizzly refused to free the port when kill()'d.
        // This will also cause problems when trying to run tests in parallel. TODO: fix.
        SERVER_STUB_PORT = SERVER_STUB_PORT+1;
        System.out.println("Launching graylog2-server stub on :" + SERVER_STUB_PORT);
        serverStub = new ServerStub(SERVER_STUB_PORT);
        serverStub.initialize();
    }

    @AfterClass
    public static void tearDownServerStub() throws Exception {
        System.out.println("Shutting down graylog2-server stub");
        serverStub.kill();
    }

    @Before
    public void setUp() throws MalformedURLException {
        String sauceUser = System.getenv("SAUCE_USERNAME");
        String saucePassword = System.getenv("SAUCE_ACCESS_KEY");

        // Decide whether to use sauceLabs or local browser to execute Selenium tests.
        if (sauceUser != null && saucePassword != null && !sauceUser.isEmpty() && !saucePassword.isEmpty()) {
            URL saucelabs = new URL("http://" + sauceUser + ":" + saucePassword + "@localhost:4445/wd/hub");

            // https://saucelabs.com/docs/platforms
            DesiredCapabilities capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability("platform", "Windows 8");
            capabilities.setCapability("version", "21");
            capabilities.setCapability("tunnel-identifier", System.getenv("TRAVIS_JOB_NUMBER"));

            driver = new RemoteWebDriver(saucelabs, capabilities);
        } else {
            driver = new FirefoxDriver();
        }

        browser = testBrowser(driver, WEB_PORT);
    }

    @After
    public void tearDown() {
        driver.quit();
    }

}
