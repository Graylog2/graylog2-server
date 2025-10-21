package org.graylog2.rest.resources.roles;

import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.rest.models.roles.responses.RolesResponse;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class RolesResourceTest {
    @Mock
    private RoleService roleService;
    @Mock
    private GlobalAuthServiceConfig globalAuthServiceConfig;

    @InjectMocks
    private RolesResource classUnderTest;

    @Test
    @WithAuthorization(permissions = { "roles:read:reader" })
    void testListAll() {
        Role readerRole = Mockito.mock(Role.class);
        Role adminRole = Mockito.mock(Role.class);

        when(readerRole.getName()).thenReturn("reader");
        when(adminRole.getName()).thenReturn("admin");
        when(roleService.loadAll()).thenReturn(Set.of(readerRole, adminRole));

        RolesResponse rolesResponse = classUnderTest.listAll();

        Set<String> roleNames = rolesResponse.roles().stream()
                .map(r -> r.name())
                .collect(Collectors.toSet());

        assertEquals(Set.of("reader"), roleNames);
    }
}
