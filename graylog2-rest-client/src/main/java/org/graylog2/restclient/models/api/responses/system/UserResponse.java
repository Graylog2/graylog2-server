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
package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.Startpage;

import java.util.List;
import java.util.Map;

public class UserResponse {

	public String username;

	@JsonProperty("full_name")
	public String fullName;

	public String id;

    public String email;

	public List<String> permissions;

    public Map<String, Object> preferences;

    public String timezone;

    @JsonProperty("read_only")
    public boolean readonly;

    public boolean external;

    public StartpageResponse startpage;

    @JsonProperty("session_timeout_ms")
    public long sessionTimeoutMs;

    public Startpage getStartpage() {
        if (startpage == null || startpage.type == null || startpage.id == null) {
            return null;
        }

        try {
            return new Startpage(Startpage.Type.valueOf(startpage.type.toUpperCase()), startpage.id);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

}
