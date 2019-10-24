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
