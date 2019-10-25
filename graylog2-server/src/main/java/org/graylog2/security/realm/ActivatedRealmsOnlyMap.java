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
package org.graylog2.security.realm;

import com.google.inject.Inject;
import org.apache.shiro.realm.AuthenticatingRealm;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides a view of the bound realms which only includes "activated" realms as configured via the
 * "activated_authentication_providers" configuration option.
 * <p>
 * If the configuration option is empty, all availalbe realms are considered to be "active" as the system would not run
 * without any authenticating realms.
 */
public class ActivatedRealmsOnlyMap extends AbstractMap<String, AuthenticatingRealm> {
    private final Map<String, AuthenticatingRealm> realms;
    private final Set<String> activatedRealmNames;

    @Inject
    public ActivatedRealmsOnlyMap(Map<String, AuthenticatingRealm> realms,
                                  @Named("activated_authentication_providers") Set<String> activatedRealmNames) {
        this.realms = realms;
        this.activatedRealmNames = activatedRealmNames;
    }

    @Override
    @Nonnull
    public Set<Entry<String, AuthenticatingRealm>> entrySet() {
        if (activatedRealmNames.isEmpty()) {
            return realms.entrySet();
        }
        return realms.entrySet()
                     .stream()
                     .filter(e -> activatedRealmNames.contains(e.getKey()))
                     .collect(Collectors.toSet());
    }
}
