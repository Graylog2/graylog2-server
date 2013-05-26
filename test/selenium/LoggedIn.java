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

import play.test.TestBrowser;
import selenium.serverstub.ServerStub;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class LoggedIn {

    protected LoggedIn() { }

    /**
     * Logs you in with a new user it creates before.
     *
     * @param browser The test browser you use in your test.
     * @return True if the login was successful.
     */
    protected Result login(TestBrowser browser, ServerStub serverStub) {
        String user = "garylog";
        String password = "123test123";
        serverStub.users.put(user, password);

        return login(browser, serverStub, user, password);
    }

    /**
     * Logs you in with the user and password you specify.
     *
     * @param browser The test browser you use in your test.
     * @param user Username to use for login
     * @param password Password to use for login
     * @return True if the login was successful.
     */
    protected Result login(TestBrowser browser, ServerStub serverStub, String user, String password) {
        browser.goTo("/login");

        browser.fill("#username").with(user);
        browser.fill("#password").with(password);
        browser.submit("#username");

        // XXX THIS SHOULD BE A POSITIVE TEST. will succeed if connection refused for example
        boolean success = !browser.url().endsWith("/login");

        return new Result(success, user, password);
    }

    public class Result {

        private final boolean success;
        private final String user;
        private final String password;

        public Result(boolean success, String user, String password) {
            this.success = success;
            this.user = user;
            this.password = password;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPassword() {
            return password;
        }

        public String getUser() {
            return user;
        }

    }

}
