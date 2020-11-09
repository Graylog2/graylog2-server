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

import com.google.common.collect.ImmutableList;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.graylog2.security.realm.AccessTokenAuthenticator;
import org.graylog2.security.realm.AuthServiceRealm;
import org.graylog2.security.realm.HTTPHeaderAuthenticationRealm;
import org.graylog2.security.realm.RootAccountRealm;
import org.graylog2.security.realm.SessionAuthenticator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Statically ordered collection of Shiro AuthenticatingRealms.
 */
@Singleton
public class StaticOrderedAuthenticatingRealms extends AbstractCollection<Realm> implements OrderedAuthenticatingRealms {
    private static final ImmutableList<String> REALM_ORDER = ImmutableList.of(
            SessionAuthenticator.NAME,
            AccessTokenAuthenticator.NAME,
            HTTPHeaderAuthenticationRealm.NAME,
            AuthServiceRealm.NAME,
            RootAccountRealm.NAME // Should come last because it's (hopefully) not used that often
    );

    private final List<Realm> orderedRealms;

    @Inject
    public StaticOrderedAuthenticatingRealms(Map<String, AuthenticatingRealm> realms) {
        this.orderedRealms = REALM_ORDER.stream()
                .map(realms::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (orderedRealms.size() < 1) {
            throw new IllegalStateException("No realms available, this must not happen!");
        }
    }

    @Nonnull
    @Override
    public Iterator<Realm> iterator() {
        return orderedRealms.iterator();
    }

    @Override
    public int size() {
        return orderedRealms.size();
    }

    @Override
    public Optional<Realm> getRootAccountRealm() {
        return orderedRealms.stream().filter(r -> r instanceof RootAccountRealm).findFirst();
    }
}
