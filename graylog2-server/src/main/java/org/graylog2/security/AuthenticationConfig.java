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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.security.realm.AccessTokenAuthenticator;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.security.realm.PasswordAuthenticator;
import org.graylog2.security.realm.RootAccountRealm;
import org.graylog2.security.realm.SessionAuthenticator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
@WithBeanGetter
public abstract class AuthenticationConfig {

    @JsonProperty("realm_order")
    public abstract List<String> realmOrder();

    @JsonProperty("disabled_realms")
    public abstract Set<String> disabledRealms();


    @JsonCreator
    public static AuthenticationConfig create(@JsonProperty("realm_order") List<String> order,
                                              @JsonProperty("disabled_realms") Set<String> disabledRealms) {
        return builder()
                .realmOrder(order)
                .disabledRealms(disabledRealms)
                .build();
    }

    public static AuthenticationConfig defaultInstance() {
        return builder()
                // the built-in default order of authenticators
                .realmOrder(ImmutableList.of(
                        SessionAuthenticator.NAME,
                        AccessTokenAuthenticator.NAME,
                        LdapUserAuthenticator.NAME,
                        PasswordAuthenticator.NAME,
                        RootAccountRealm.NAME))
                .disabledRealms(Collections.emptySet())
                .build();
    }

    public AuthenticationConfig withRealms(final Set<String> availableRealms) {
        final List<String> newOrder = new ArrayList<>();

        // Check if realm actually exists.
        realmOrder().stream()
                .filter(availableRealms::contains)
                .forEach(newOrder::add);

        // Add availableRealms which are not in the config yet to the end.
        availableRealms.stream()
                .filter(realm -> !newOrder.contains(realm))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(newOrder::add);

        return toBuilder().realmOrder(newOrder).build();
    }

    public abstract Builder toBuilder();

    private static Builder builder() {
        return new AutoValue_AuthenticationConfig.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder realmOrder(List<String> order);

        public abstract Builder disabledRealms(Set<String> disabledRealms);

        public abstract AuthenticationConfig build();
    }

}
