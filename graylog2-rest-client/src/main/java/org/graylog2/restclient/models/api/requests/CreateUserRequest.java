/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.requests;

import org.graylog2.restclient.models.User;
import org.joda.time.DateTimeZone;

import static play.data.validation.Constraints.Required;

public class CreateUserRequest extends ChangeUserRequest {
    @Required
    public String username;
    @Required
    public String password;

    public CreateUserRequest() { /* required for data binding */ }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CreateUserRequest(User user) {
        this.username = user.getName();
        this.fullname = user.getFullName();
        this.email = user.getEmail();
        this.password = "";
        this.permissions  = user.getPermissions();
        final DateTimeZone timeZone = user.getTimeZone();
        if (timezone != null) {
            this.timezone = timeZone.getID();
        }
        this.sessionTimeoutMs = user.getSessionTimeoutMs();
    }

    public CreateUserRequest(CreateUserRequest c) {
        username = c.username;
        password = c.password;
        fullname = c.fullname;
        email = c.email;
        permissions = c.permissions;
        timezone = c.timezone;
        startpage = c.startpage;
        sessionTimeoutMs = c.sessionTimeoutMs;
    }
}
