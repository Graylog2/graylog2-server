package org.graylog.integrations.dbconnector;


import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorInputTest {
    // Code Under Test
    DBConnectorInput cut;
    DBConnectorInput.Config cutConfig;

    // Mock Objects
    @Mock(answer = Answers.RETURNS_MOCKS)
    DBConnectorTransport.Factory mockTransportFactory;
    @Mock
    LocalMetricRegistry mockLocalRegistry;
    @Mock(answer = Answers.RETURNS_MOCKS)
    DBConnectorCodec.Factory mockCodecFactory;


    // Test Objects
    ConfigurationRequest configurationRequest;

    // Setup and tear down
    @Before
    public void setUp() {
        cutConfig = new DBConnectorInput.Config(mockTransportFactory, mockCodecFactory);
    }

    // Test Cases
    @Test
    public void doLaunch_throwsMisfireException_whenImplementationNotDone() {
        givenClassNotImplementedYet();

        whenCombinedRequestedConfigurationIsCalled();

        thenConfigurationIsNotNull();
    }

    // GIVENs
    private void givenClassNotImplementedYet() {

    }

    // WHENs
    private void whenCombinedRequestedConfigurationIsCalled() {
        configurationRequest = cutConfig.combinedRequestedConfiguration();
    }

    // THENs
    private void thenConfigurationIsNotNull() {
        assertThat(configurationRequest, notNullValue());
    }
}
