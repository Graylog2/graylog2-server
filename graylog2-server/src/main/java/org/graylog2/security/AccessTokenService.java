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
package org.graylog2.security;

import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.ValidationException;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface AccessTokenService extends PersistedService {
    @SuppressWarnings("unchecked")
    AccessToken load(String token);

    @Nullable
    AccessToken loadById(String id);

    @SuppressWarnings("unchecked")
    List<AccessToken> loadAll(String username);

    AccessToken create(String username, String name);

    void touch(AccessToken accessToken) throws ValidationException;

    String save(AccessToken accessToken) throws ValidationException;

    int deleteAllForUser(String username);
}
