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

import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.users.DefaultPasswordAlgorithm;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

public class PasswordAlgorithmFactory {
    private final Map<String, PasswordAlgorithm> passwordAlgorithms;
    private final PasswordAlgorithm defaultPasswordAlgorithm;

    @Inject
    public PasswordAlgorithmFactory(Map<String, PasswordAlgorithm> passwordAlgorithms,
                                    @DefaultPasswordAlgorithm PasswordAlgorithm defaultPasswordAlgorithm) {
        this.passwordAlgorithms = passwordAlgorithms;
        this.defaultPasswordAlgorithm = defaultPasswordAlgorithm;
    }

    @Nullable
    public PasswordAlgorithm forPassword(String hashedPassword) {
        for (PasswordAlgorithm passwordAlgorithm : passwordAlgorithms.values()) {
            if (passwordAlgorithm.supports(hashedPassword))
                return passwordAlgorithm;
        }

        return null;
    }

    @Nullable
    public PasswordAlgorithm forName(String name) {
        return this.passwordAlgorithms.get(name);
    }

    public PasswordAlgorithm defaultPasswordAlgorithm() {
        return defaultPasswordAlgorithm;
    }
}
