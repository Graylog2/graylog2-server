/**
 * Copyright 2013 Kay Roepke <lennart@torch.sh>
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
package selenium.pages;

import controllers.routes;
import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.Page;

import static org.fest.assertions.Assertions.assertThat;

public class LoginPage extends FluentPage {
    private final static String usernameElement = "#username";
    private final static String passwordElement = "#password";
    private final static String submitElement = "#signin";

    @Page
    public DashboardPage dashboardPage;

    @Override
    public String getUrl() {
        return routes.SessionsController.index().url();
    }

    @Override
    public void isAt() {
        assertThat(title()).contains("Sign in");
    }

    public DashboardPage loginAs(String username, String password) {
        fill(usernameElement).with(username);
        fill(passwordElement).with(password);
        submit(submitElement);
        return dashboardPage;
    }

    public LoginPage loginWithError(String username, String password) {
        fill(usernameElement).with(username);
        fill(passwordElement).with(password);
        submit(submitElement);
        return this;
    }
}
