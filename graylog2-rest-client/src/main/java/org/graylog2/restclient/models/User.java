/**
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
 */
package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.ChangePasswordRequest;
import org.graylog2.restclient.models.api.requests.ChangeUserRequest;
import org.graylog2.restclient.models.api.responses.system.UserResponse;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class User {
	private static final Logger log = LoggerFactory.getLogger(User.class);

    private final ApiClient api;
    @Deprecated
    private final String id;
    private final String name;
	private final String email;
	private final String fullName;
	private final List<String> permissions;
    private final String sessionId;
    private final DateTimeZone timezone;
    private final boolean readonly;
    private final boolean external;
    private final Startpage startpage;
    private final long sessionTimeoutMs;
    private final Map<String, Object> preferences;

    private Subject subject;

    @AssistedInject
    public User(ApiClient api, @Assisted UserResponse ur, @Nullable @Assisted String sessionId) {
        this(api, ur.id, ur.username, ur.email, ur.fullName, ur.permissions, sessionId, ur.timezone, ur.readonly, ur.external, ur.getStartpage(), ur.sessionTimeoutMs, ur.preferences);
    }

	public User(ApiClient api,
                String id,
                String name,
                String email,
                String fullName,
                List<String> permissions,
                String sessionId,
                String timezone,
                boolean readonly,
                boolean external,
                Startpage startpage,
                long sessionTimeoutMs,
                Map<String, Object> preferences) {
        DateTimeZone timezone1 = null;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.api = api;
        this.id = id;
        this.name = name;
		this.email = email;
		this.fullName = fullName;
		this.permissions = permissions;
        this.sessionId = sessionId;
        try {
            if (timezone != null) {
                timezone1 = DateTimeZone.forID(timezone);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid time zone name {} when loading user {}.", timezone, name);
        } finally {
            this.timezone = timezone1;
        }
        this.readonly = readonly;
        this.external = external;
        this.startpage = startpage;
        if (preferences != null) {
            this.preferences = preferences;
        } else {
            this.preferences = Collections.emptyMap();
        }
    }

    public boolean update(ChangeUserRequest request) {
        try {
            api.path(routes.UsersResource().changeUser(getName())).body(request).expect(Http.Status.NO_CONTENT).execute();
            return true;
        } catch (APIException e) {
            log.error("Unable to update user", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to update user", e);
            return false;
        }
    }
    @Deprecated
    public String getId() {
        return getName();
    }

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public List<String> getPermissions() {
        if (permissions == null) {
            return Collections.emptyList();
        }
		return permissions;
	}

    public String getSessionId() {
        return sessionId;
    }

    public DateTimeZone getTimeZone() {
        return timezone;
    }

    public boolean updatePassword(ChangePasswordRequest request) {
        try {
            api.path(routes.UsersResource().changePassword(getName()))
                .body(request)
                .expect(Http.Status.NO_CONTENT)
                .execute();
        } catch (APIException e) {
            log.error("Unable to update password", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to update password", e);
            return false;
        }
        return true;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public boolean isExternal() {
        return external;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        if (subject == null) {
            // TODO we should do this cleanly via shiro, but time is too short. clean up post-RC
            return new Subject.Builder(SecurityUtils.getSecurityManager())
                    .principals(new SimplePrincipalCollection(getName(), "REST realm"))
                    .authenticated(true)
                    .buildSubject();
        }
        return subject;
    }

    public boolean setStartpage(Startpage startpage) {
        ChangeUserRequest cur = new ChangeUserRequest(this);

        if (startpage == null) {
            cur.startpage.type = null;
            cur.startpage.id = null;
        } else {
            cur.startpage.type = startpage.getType().toString().toLowerCase();
            cur.startpage.id = startpage.getId();
        }
        return update(cur);
    }

    public long getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public interface Factory {
        User fromResponse(UserResponse ur, String sessionId);
    }

    public Startpage getStartpage() {
        return startpage;
    }

}
