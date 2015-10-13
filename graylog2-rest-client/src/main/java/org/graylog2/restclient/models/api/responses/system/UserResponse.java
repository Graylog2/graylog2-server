/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.Startpage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public Set<String> roles;

    public Startpage getStartpage() {
        if (startpage == null || startpage.type == null || startpage.id == null) {
            return null;
        }

        try {
            return new Startpage(Startpage.Type.valueOf(startpage.type.toUpperCase(Locale.ENGLISH)), startpage.id);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

}
