package org.graylog2.security.realm;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ActivatedRealmsOnlyMapTest {

    private final Map<String, AuthenticatingRealm> allRealms = ImmutableMap.of(
            "realm-1", Mockito.mock(AuthenticatingRealm.class),
            "realm-2", Mockito.mock(AuthenticatingRealm.class),
            "realm-3", Mockito.mock(AuthenticatingRealm.class)
    );

    @Test
    public void noActivatedRealmNames() {
        ActivatedRealmsOnlyMap map = new ActivatedRealmsOnlyMap(allRealms, Collections.emptySet());
        assertEquals(allRealms, map);
    }

    @Test
    public void onlyActivatedRealms() {
        ActivatedRealmsOnlyMap map = new ActivatedRealmsOnlyMap(allRealms, Collections.singleton("realm-2"));
        assertEquals(ImmutableMap.of("realm-2", allRealms.get("realm-2")), map);
    }
}
