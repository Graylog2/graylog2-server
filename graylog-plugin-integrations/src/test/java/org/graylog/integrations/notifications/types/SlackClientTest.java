package org.graylog.integrations.notifications.types;

import com.github.joschi.jadconfig.util.Duration;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog2.shared.bindings.providers.OkHttpClientProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;


@Ignore("Build is failing on jenkins")
public class SlackClientTest {

    private SlackClient okHttpSlackClient;

    private final MockWebServer server = new MockWebServer();

    @Before
    public void setUp() throws IOException {
      server.start();
      okHttpSlackClient = new SlackClient(getOkHttpClient());

    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test(expected = PermanentEventNotificationException.class)
    public void send_throwsException_whenWebhookUrlInvalid() throws Exception {
        SlackMessage message = new SlackMessage("Henry Hühnchen(little chicken)");
        SlackEventNotificationConfig slackEventNotificationConfig = SlackEventNotificationConfig.builder()
                .webhookUrl("http://localhost:8080")
                .build();
        okHttpSlackClient.send(message,slackEventNotificationConfig.webhookUrl());
    }

    OkHttpClient getOkHttpClient() {

        final OkHttpClientProvider provider = new OkHttpClientProvider(
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                server.url("/").uri(),
                null);

        return provider.get();

    }




}
