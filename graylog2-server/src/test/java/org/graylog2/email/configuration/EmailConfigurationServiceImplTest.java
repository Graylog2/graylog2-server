package org.graylog2.email.configuration;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EmailConfigurationServiceImplTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private EmailConfigurationServiceImpl emailConfigurationService;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() {
        emailConfigurationService = new EmailConfigurationServiceImpl(clusterConfigService);
    }

    @Test
    public void loadAndReturnWithoutModifying() {
        EmailConfiguration emailConfiguration = mock(EmailConfiguration.class);
        when(clusterConfigService.get(EmailConfiguration.class)).thenReturn(emailConfiguration);

        EmailConfiguration result = emailConfigurationService.load();

        verify(clusterConfigService, times(1)).get(EmailConfiguration.class);
        assertThat(result).isEqualTo(emailConfiguration);
    }

    @Test
    public void loadAndReturnNullIfNotFound() {
        when(clusterConfigService.get(EmailConfiguration.class)).thenReturn(null);

        EmailConfiguration result = emailConfigurationService.load();

        assertThat(result).isNull();
    }

    @Test
    public void save() {
        EmailConfiguration emailConfiguration = mock(EmailConfiguration.class);

        emailConfigurationService.save(emailConfiguration);

        verify(clusterConfigService, times(1)).write(emailConfiguration);
    }
}
