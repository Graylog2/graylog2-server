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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.UserAuthorizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EntityDependencyPermissionCheckerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private UserAuthorizer.Factory userAuthorizerFactory;

    private EntityDependencyPermissionChecker resolver;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Before
    public void setUp() throws Exception {
        this.resolver = new EntityDependencyPermissionChecker(userAuthorizerFactory, new BuiltinCapabilities());
    }

    @Test
    public void checkWithPermitted() {
        // Read is permitted on the stream so we expect to receive an empty result from the check
        assertThat(runCheck(true, true)).satisfies(result -> {
            assertThat(result.values()).isEmpty();
        });
    }

    @Test
    public void checkWithDenied() {
        // Read is NOT permitted on the stream so we expect to receive the stream in the result
        assertThat(runCheck(true, false)).satisfies(result -> {
            assertThat(result.values()).isNotEmpty();
        });
    }

    @Test
    public void checkWithDeniedAndSharingUserDenied() {
        // Since the sharing user cannot view the dependency, we don't return it in the result even though the
        // grantee cannot access it (to avoid exposing the dependency)
        assertThat(runCheck(false, true)).satisfies(result -> {
            assertThat(result.values()).isEmpty();
        });
    }

    private ImmutableMultimap<GRN, EntityDependency> runCheck(boolean isSharingUserAuthorized, boolean isGranteeUserAuthorized) {
        final GRN granteeUser = grnRegistry.newGRN("user", "john");
        final GRN sharingUser = grnRegistry.newGRN("user", "jane");
        final GRN stream = grnRegistry.newGRN("stream", "54e3deadbeefdeadbeef0001");
        final UserAuthorizer sharingUserAuthorizer = mock(UserAuthorizer.class);
        final UserAuthorizer granteeUserAuthorizer = mock(UserAuthorizer.class);
        final ImmutableSet<GRN> selectedGrantees = ImmutableSet.of(granteeUser);
        final EntityDependency dependency = EntityDependency.create(stream, "Title", ImmutableSet.of());
        final ImmutableSet<EntityDependency> dependencies = ImmutableSet.of(dependency);

        when(userAuthorizerFactory.create(sharingUser)).thenReturn(sharingUserAuthorizer);
        when(userAuthorizerFactory.create(granteeUser)).thenReturn(granteeUserAuthorizer);

        when(sharingUserAuthorizer.isPermitted(anyString(), any(GRN.class))).thenReturn(isSharingUserAuthorized);
        when(granteeUserAuthorizer.isPermitted("streams:read", stream)).thenReturn(isGranteeUserAuthorized);

        final ImmutableMultimap<GRN, EntityDependency> checkResult = resolver.check(sharingUser, dependencies, selectedGrantees);

        verify(sharingUserAuthorizer, times(1)).isPermitted("streams:read", stream);
        verifyNoMoreInteractions(sharingUserAuthorizer);

        return checkResult;
    }
}
