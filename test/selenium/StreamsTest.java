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
import org.junit.Before;
import org.junit.Test;
import play.libs.F;
import play.test.TestBrowser;
import selenium.serverstub.ServerStub;

import static org.junit.Assert.assertTrue;
import static play.test.Helpers.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamsTest extends LoggedIn {

    private static final int STUB_PORT = 9005;
    private ServerStub serverStub;

    @Before
    public void setUp() {
        System.out.println("Launching graylog2-server stub on :" + STUB_PORT);
        serverStub = new ServerStub(STUB_PORT);
        serverStub.initialize();
    }

    @After
    public void tearDown() {
        System.out.println("Shutting down graylog2-server stub");
        serverStub.kill();
    }

    @Test
    public void addingStreamRulesWorks() {
        running(testServer(19001), FIREFOX, new F.Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                Result r = login(browser, serverStub, "lennart", "123123123");
                //Result r = login(browser, serverStub);
                assertTrue("Login failed", r.isSuccess());

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
    }

}
