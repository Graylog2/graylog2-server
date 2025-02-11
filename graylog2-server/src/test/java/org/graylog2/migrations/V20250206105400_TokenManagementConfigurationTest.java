package org.graylog2.migrations;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.UserConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class V20250206105400_TokenManagementConfigurationTest {
    //We prepare some existing config with explicitly updated values, so we can safely check they're not touched by the migration:
    private final UserConfiguration existingConfig = UserConfiguration.create(true, Duration.of(10, ChronoUnit.HOURS), true, false);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private ClusterConfigService configService;

    private V20250206105400_TokenManagementConfiguration testee;

    @Before
    public void setUp() {
        testee = new V20250206105400_TokenManagementConfiguration(configService, false);
    }

    @Test
    public void doNothingIfMigrationAlreadyRanSuccessfully() {
        setupMocks(true, false);

        testee.upgrade();

        verify(configService).get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class);
        verifyNoMoreInteractions(configService);
    }

    @Test
    public void newInstallationInsertsFullUserConfigWithDefaultValues() {
        //This time, it is a fresh installation:
        testee = new V20250206105400_TokenManagementConfiguration(configService, true);
        setupMocks(false, false);

        testee.upgrade();

        verify(configService).get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class);
        verify(configService).write(UserConfiguration.DEFAULT_VALUES);
        verify(configService).write(new V20250206105400_TokenManagementConfiguration.MigrationCompleted());
        verifyNoMoreInteractions(configService);
    }

    @Test
    public void upgradeWithoutConfigWritesConservativeDefaults() {
        setupMocks(false, false);

        testee.upgrade();

        verify(configService).get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class);
        verify(configService).get(UserConfiguration.class);
        verify(configService).write(UserConfiguration.DEFAULT_VALUES_FOR_UPGRADE);
        verify(configService).write(new V20250206105400_TokenManagementConfiguration.MigrationCompleted());
        verifyNoMoreInteractions(configService);
    }

    @Test
    public void existingConfigIsUpdatedWithDefaultValuesOnUpgrade() {
        setupMocks(false, true);
        //Expected to be written - keeps existing values for globalSessionTimeout and -interval, but applies default values for token access management
        final UserConfiguration updated = UserConfiguration.create(existingConfig.enableGlobalSessionTimeout(),
                existingConfig.globalSessionTimeoutInterval(),
                UserConfiguration.DEFAULT_VALUES_FOR_UPGRADE.allowAccessTokenForExternalUsers(),
                UserConfiguration.DEFAULT_VALUES_FOR_UPGRADE.restrictAccessTokenToAdmins());

        testee.upgrade();

        verify(configService).get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class);
        verify(configService).get(UserConfiguration.class);
        verify(configService).write(updated);
        verify(configService).write(new V20250206105400_TokenManagementConfiguration.MigrationCompleted());
        verifyNoMoreInteractions(configService);
    }


    private void setupMocks(boolean migrationAlreadyRan, boolean configExists) {
        if (migrationAlreadyRan) {
            when(configService.get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class)).thenReturn(new V20250206105400_TokenManagementConfiguration.MigrationCompleted());
        } else {
            when(configService.get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class)).thenReturn(null);
            when(configService.get(UserConfiguration.class)).thenReturn(configExists ? existingConfig : null);
        }
    }
}
