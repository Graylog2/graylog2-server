package org.graylog2.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog2.security.realm.AccessTokenAuthenticator;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.security.realm.PasswordAuthenticator;
import org.graylog2.security.realm.RootAccountRealm;
import org.graylog2.security.realm.SessionAuthenticator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
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
