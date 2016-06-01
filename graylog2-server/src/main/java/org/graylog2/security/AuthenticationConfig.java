package org.graylog2.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog2.security.realm.AccessTokenAuthenticator;
import org.graylog2.security.realm.GraylogSimpleAccountRealm;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.security.realm.PasswordAuthenticator;
import org.graylog2.security.realm.SessionAuthenticator;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class AuthenticationConfig {

    @JsonProperty("realm_order")
    public abstract List<String> realmOrder();


    @JsonCreator
    public AuthenticationConfig create(@JsonProperty("realm_order") List<String> order) {
        return builder()
                .realmOrder(order)
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
                        GraylogSimpleAccountRealm.NAME))
                .build();
    }

    private static Builder builder() {
        return new AutoValue_AuthenticationConfig.Builder();
    }

    @AutoValue.Builder
    public abstract class Builder {

        public abstract Builder realmOrder(List<String> order);
        public abstract AuthenticationConfig build();
    }

}
