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

import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface AccessToken extends Persisted {
    enum Type {
        PLAINTEXT(0), AESSIV(1);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        public int getIntValue() {
            return type;
        }

        public static Type defaultType() {
            return AESSIV;
        }
    }

    DateTime getLastAccess();

    String getUserName();

    void setUserName(String userName);

    String getToken();

    void setToken(String token);

    String getName();

    void setName(String name);
}
