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

import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityOwnershipServiceTest {

    private EntityOwnershipService entityOwnershipService;
    private DBGrantService dbGrantService;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @BeforeEach
    void setUp() {
        this.dbGrantService = mock(DBGrantService.class);
        this.entityOwnershipService = new EntityOwnershipService(dbGrantService, grnRegistry);
    }

    @Test
    void registerNewView() {
        final User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("mockuser");

        entityOwnershipService.registerNewEventDefinition("1234", mockUser);

        ArgumentCaptor<GrantDTO> grant = ArgumentCaptor.forClass(GrantDTO.class);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(dbGrantService).create(grant.capture(), user.capture());

        assertThat(grant.getValue()).satisfies(g -> {
            assertThat(g.capability()).isEqualTo(Capability.OWN);
            assertThat(g.target().type()).isEqualTo(GRNTypes.EVENT_DEFINITION.type());
            assertThat(g.target().entity()).isEqualTo("1234");
            assertThat(g.grantee().type()).isEqualTo(GRNTypes.USER.type());
            assertThat(g.grantee().entity()).isEqualTo("mockuser");
        });
    }
}
