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
package selenium.tests.system;

import com.google.common.collect.Maps;
import lib.Configuration;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import selenium.LoggedIn;

import java.util.Map;

import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ServerMessagesTest extends LoggedIn {

    @Test
    public void showsServerMessages() {
        running(testServer(WEB_PORT), new Runnable() {
            public void run() {
                Configuration.setServerRestUris("http://127.0.0.1:" + SERVER_STUB_PORT);

                serverStub.addSystemMessage(
                        "solveigPlease",
                        "test message 1",
                        new DateTime(),
                        "foo-bar-123"
                );

                serverStub.addSystemMessage(
                        "testClass",
                        "test message 2",
                        new DateTime(),
                        "foo-9001"
                );

                doLogin();
                browser.goTo("/system");

                assertEquals(1, browser.find(".system-messages").size());
                assertEquals(2, browser.find(".system-messages tbody tr").size());

                assertEquals("foo-bar-123", browser.find(".system-messages tbody tr", 0).find("td", 1).html());
                assertEquals("test message 1", browser.find(".system-messages tbody tr", 0).find("td", 2).html());

                assertEquals("foo-9001", browser.find(".system-messages tbody tr", 1).find("td", 1).html());
                assertEquals("test message 2", browser.find(".system-messages tbody tr", 1).find("td", 2).html());
            }
        });
    }

}
