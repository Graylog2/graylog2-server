package org.graylog.security;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
    }

    @Test
    void UserContextWithoutContext() {
        assertThatExceptionOfType(UserContextMissingException.class).isThrownBy(() -> new UserContext.Factory(userService).create());
    }

    @Test
    void runAs() {
        DefaultSecurityManager sm = new DefaultSecurityManager();
        SecurityUtils.setSecurityManager(sm);
        final User user = new UserImpl(mock(PasswordAlgorithmFactory.class), mock(Permissions.class), ImmutableMap.of());
        when(userService.load(anyString())).thenReturn(user);

        final String USERNAME = "unknown-user";
        UserContext.<Void>runAs(USERNAME, () -> {

            final UserContext userContext = new UserContext.Factory(userService).create();
            assertThat(userContext.getUsername()).isEqualTo(USERNAME);
            assertThat(userContext.getUser().get()).isEqualTo(user);

            return null;
        });
    }
}
