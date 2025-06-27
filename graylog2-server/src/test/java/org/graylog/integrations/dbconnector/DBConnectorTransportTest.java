package org.graylog.integrations.dbconnector;

import org.graylog.integrations.dbconnector.external.DBConnectorClient;
import org.graylog.integrations.dbconnector.external.DBConnectorClientFactory;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_POLLING_INTERVAL;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_POLLING_TIME_UNIT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorTransportTest {

    // Code Under Test
    @InjectMocks
    DBConnectorTransport cut;

    // Mock Objects
    @Mock
    Configuration mockConfiguration;
    @Mock
    DBConnectorClientFactory mockDBConnectorClientFactory;
    @Mock
    ScheduledExecutorService mockExecutorService;
    @Mock
    DBConnectorInput mockDBConnectorInput;
    @Mock
    ScheduledFuture mockRunningTask;
    @Mock
    DBConnectorClient mockDBConnectorClient;
    @Mock
    InputFailureRecorder mockInputFailureRecorder;

    // Test objects
    private static final String TEST_CONNECTION_STRING = "Connection String";
    private static final String TEST_DB_TYPE = "Oracle";

    // Test Cases
    @Test
    public void doLaunch_schedulesPollerTask_whenConfigurationIsValid() throws Exception {
        givenGoodConfiguration(5);
        givenGoodClientFactory();
        givenGoodExecutorService();
        whenDoLaunchIsCalled();
        thenClientFactoryInvokedAsExpected();
    }

    @Test
    public void doStop_terminatesPollerTask_whenTaskIsRunning() throws Exception {
        givenGoodConfiguration(5);
        givenGoodClientFactory();
        givenGoodExecutorService();
        whenDoLaunchIsCalled();

        thenClientFactoryInvokedAsExpected();
        thenTaskSubmittedToExecutor(5L);

        whenDoStopIsCalled();

        thenRunningTaskIsCancelled();
    }

    @Test
    public void doStop_doesNothing_whenTaskIsNotRunning() {
        whenDoStopIsCalled();

        thenRunningTaskIsNotCancelled();
    }

    @Test(expected = MisfireException.class)
    public void doLaunch_throwsMisfireException_whenClientFactoryFails() throws Exception {
        givenGoodConfiguration(5);
        givenClientFactoryFails();

        whenDoLaunchIsCalled();
    }

    // GIVENs
    private void givenGoodConfiguration(int pollingInterval) {
        given(mockConfiguration.getString(eq(CK_DATABASE_TYPE))).willReturn(TEST_DB_TYPE);
        given(mockConfiguration.getInt(eq(CK_POLLING_INTERVAL))).willReturn(pollingInterval);
        given(mockConfiguration.getString(eq(CK_POLLING_TIME_UNIT))).willReturn(MINUTES.name());
        given(mockDBConnectorInput.getConfiguration()).willReturn(mockConfiguration);
    }

    private void givenGoodClientFactory() throws Exception {
        given(mockDBConnectorClientFactory.getClient(any()))
                .willReturn(mockDBConnectorClient);
    }

    private void givenClientFactoryFails() throws Exception {
        given(mockDBConnectorClientFactory.getClient(anyString()))
                .willThrow(new MalformedURLException());
    }

    private void givenGoodExecutorService() {
        given(mockExecutorService.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mockRunningTask);
    }

    // WHENs
    private void whenDoLaunchIsCalled() throws MisfireException {
        cut.doLaunch(mockDBConnectorInput, mockInputFailureRecorder);
    }

    private void whenDoStopIsCalled() {
        cut.doStop();
    }

    // THENs
    private void thenClientFactoryInvokedAsExpected() throws Exception {

        ArgumentCaptor<String> dbTypeCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockDBConnectorClientFactory, times(1)).getClient(dbTypeCaptor.capture());
        assertThat(dbTypeCaptor.getValue(), is(TEST_DB_TYPE));

    }

    private void thenTaskSubmittedToExecutor(Long pollingInterval) {
        ArgumentCaptor<DBConnectorPollerTask> taskCaptor = ArgumentCaptor.forClass(DBConnectorPollerTask.class);
        ArgumentCaptor<Long> initialDelayCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> intervalCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(mockExecutorService, times(1)).scheduleWithFixedDelay(taskCaptor.capture(),
                initialDelayCaptor.capture(), intervalCaptor.capture(), timeUnitCaptor.capture());

        assertThat(initialDelayCaptor.getValue(), is(0L));
        assertThat(intervalCaptor.getValue(), is(pollingInterval));
        assertThat(timeUnitCaptor.getValue(), is(MINUTES));
    }

    private void thenRunningTaskIsCancelled() {
        ArgumentCaptor<Boolean> mayInterruptCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(mockRunningTask, times(1)).cancel(mayInterruptCaptor.capture());

        assertThat(mayInterruptCaptor.getValue(), is(true));
    }

    private void thenRunningTaskIsNotCancelled() {
        verify(mockRunningTask, times(0)).cancel(anyBoolean());
    }

}
