/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package selenium.tests.sessions;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.InputLaunchRequest;
import models.api.responses.cluster.NodeSummaryResponse;
import models.api.responses.system.InputSummaryResponse;
import models.api.responses.system.InputsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;
import org.fluentlenium.adapter.FluentTest;
import org.fluentlenium.adapter.util.SharedDriver;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import play.mvc.Http;
import play.test.FakeApplication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.fakeApplication;

@SharedDriver(type = SharedDriver.SharedType.PER_CLASS)
public class BaseSeleniumTest extends FluentTest {
    public static final int WEB_PORT = 9999;

    private static Client client;
    private ApiClient api;

    @Override
    public String getDefaultBaseUrl() {
        return "http://localhost:" + WEB_PORT + "/";
    }

    @Override
    public WebDriver getDefaultDriver() {
        String sauceUser = System.getenv("SAUCE_USERNAME");
        String saucePassword = System.getenv("SAUCE_ACCESS_KEY");

        // Decide whether to use sauceLabs or local browser to execute Selenium tests.
        RemoteWebDriver driver;
        if (sauceUser != null && saucePassword != null && !sauceUser.isEmpty() && !saucePassword.isEmpty()) {
            URL saucelabs = null;
            try {
                saucelabs = new URL("http://" + sauceUser + ":" + saucePassword + "@localhost:4445/wd/hub");
            } catch (MalformedURLException e) {
                // ignore
            }

            // https://saucelabs.com/docs/platforms
            DesiredCapabilities capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability("platform", "Windows 8");
            capabilities.setCapability("version", "21");
            capabilities.setCapability("tunnel-identifier", System.getenv("TRAVIS_JOB_NUMBER"));

            driver = new RemoteWebDriver(saucelabs, capabilities);
        } else {
            driver = new FirefoxDriver();
        }
        return driver;
    }

    protected FakeApplication getApp() {
        System.setProperty("skip.config.check", "true");
        Map<String, Object> options = Maps.newHashMap();
        options.put("application.secret", "qwertyqwertyqwertyqwerty");
        options.put("graylog2-server.uris", "http://localhost:12900");
        return fakeApplication(options);
    }

    protected synchronized static Client esClient() {
        if (client == null) {
            final NodeBuilder builder = NodeBuilder.nodeBuilder().client(true);
            if (System.getenv("TRAVIS").equals("true")) {
                builder.clusterName("elasticsearch");
            } else {
                builder.clusterName("graylog2");
            }
            builder.settings()
                    .put("node.name", "selenium-test-client")
                    .put("http.enabled", "false")
                    .put("discovery.zen.ping.multicast.enabled", "false")
                    .put("discovery.zen.ping.unicast.hosts", "localhost:9300");
            client = builder.node().client();
        }
        return client;
    }

    protected void apiFail(Throwable e) {
        fail("API Request should not fail.", e);
    }

    protected HostAndPort ensureTcpGelfInput() {
        HostAndPort gelfTcpAddr = null;
        try {
            final NodeSummaryResponse nodeResponse = api().get(NodeSummaryResponse.class).path("/system/cluster/node").execute();
            final URI uri = URI.create(nodeResponse.transportAddress);
            final HostAndPort nodeAddr = HostAndPort.fromParts(uri.getHost(), 12201);
            final InputsResponse ir = api().get(InputsResponse.class).path("/system/inputs").execute();
            if (ir.total > 0) {
                for (InputSummaryResponse input : ir.inputs) {
                    if (!input.type.equals("org.graylog2.inputs.gelf.tcp.GELFTCPInput")) {
                        continue;
                    }
                    gelfTcpAddr = HostAndPort.fromParts(
                            input.attributes.get("bind_address").toString(),
                            Double.valueOf(input.attributes.get("port").toString()).intValue() // Sad, very sad.
                    );
                }
            }
            // there was no existing tcp input, we need to create one
            if (gelfTcpAddr == null) {
                // set up new GELF TCP input
                final InputLaunchRequest launchRequest = new InputLaunchRequest();
                launchRequest.creatorUserId = "admin";
                launchRequest.title = "GELF TCP input for Selenium tests";
                launchRequest.type = "org.graylog2.inputs.gelf.tcp.GELFTCPInput";
                final HashMap<String, Object> config = Maps.newHashMap();
                config.put("port", nodeAddr.getPort());
                config.put("bind_address", nodeAddr.getHostText());
                launchRequest.configuration = config;
                api().post()
                        .path("/system/inputs")
                        .body(launchRequest)
                        .expect(Http.Status.ACCEPTED)
                        .execute();
                gelfTcpAddr = nodeAddr;
            }
        } catch (APIException | IOException e) {
            apiFail(e);
        }
        return gelfTcpAddr;
    }

    protected synchronized ApiClient api() {
        if (api == null) {
            api = lib.Global.getInjector().getInstance(ApiClient.class);
        }
        return api;
    }

    protected Channel connectToGraylog2Gelf(HostAndPort tcpInputAddr) {
        ClientBootstrap gelfClient = new ClientBootstrap(new NioClientSocketChannelFactory());
        final ChannelFuture future = gelfClient.connect(new InetSocketAddress(tcpInputAddr.getHostText(), tcpInputAddr.getPort()));
        final Channel channel = future.awaitUninterruptibly().getChannel();
        assertThat(channel.isConnected()).isTrue();
        return channel;
    }

    protected void sendGelfMessage(Channel channel, Map<String, Object> gelfMap) {
        final String gelf = new Gson().toJson(gelfMap);
        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(
                ChannelBuffers.wrappedBuffer(gelf.getBytes(Charsets.UTF_8)),
                Delimiters.nulDelimiter()[0]);
        channel.write(buffer).awaitUninterruptibly().addListener(ChannelFutureListener.CLOSE);
    }
}
