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

import java.util.concurrent.TimeUnit;

public class CreateUserRequestForm extends CreateUserRequest {

    // these exist for form binding, not existent in API!
    public boolean admin;

    public boolean session_timeout_never = false;

    public long timeout = 8;

    public String timeout_unit = "hours";

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isSession_timeout_never() {
        return session_timeout_never;
    }

    public void setSession_timeout_never(boolean session_timeout_never) {
        this.session_timeout_never = session_timeout_never;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getTimeout_unit() {
        return timeout_unit;
    }

    public void setTimeout_unit(String timeout_unit) {
        this.timeout_unit = timeout_unit;
    }

    public CreateUserRequest toApiRequest() {
        final CreateUserRequest request = new CreateUserRequest(this);
        // -1 is "never"
        if (session_timeout_never) {
            request.sessionTimeoutMs = -1;
        } else {
            request.sessionTimeoutMs = TimeUnit.valueOf(timeout_unit.toUpperCase()).toMillis(timeout);
        }
        return request;
    }
}
