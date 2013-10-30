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

import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Uninterruptibles;
import org.elasticsearch.ElasticSearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.domain.FluentWebElement;
import org.jboss.netty.channel.Channel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import selenium.pages.DashboardPage;
import selenium.pages.LoginPage;
import selenium.pages.SearchPage;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

public class SearchTest extends BaseSeleniumTest {
    private static final Logger log = LoggerFactory.getLogger(SearchTest.class);
    private static final Client client = esClient();

    @Page
    LoginPage loginPage;

    @BeforeClass
    public static void setupTest() {
        try {
            client.admin().cluster().health(new ClusterHealthRequest().waitForYellowStatus()).actionGet(5, TimeUnit.SECONDS);
            // remove all messages we might have sent
            client.deleteByQuery(new DeleteByQueryRequest("graylog2_*").query(QueryBuilders.matchAllQuery())).actionGet();
        } catch (ElasticSearchTimeoutException e) {
            assertThat(false).describedAs("ElasticSearch is not available.").isTrue(); // umm, yeah well
        }
    }

    @AfterClass
    public static void cleanup() {
        // remove all messages we might have sent
        //client.deleteByQuery(new DeleteByQueryRequest("graylog2_*").query(QueryBuilders.matchAllQuery())).actionGet();
    }

    @Test
    public void defaultSearch() {
        running(testServer(WEB_PORT, getApp()), new Runnable() {
            @Override
            public void run() {
                final HostAndPort tcpInputAddr = ensureTcpGelfInput();
                loginPage.go();
                final DashboardPage dashboardPage = loginPage.loginAs("admin", "admin");
                final SearchPage searchResult = dashboardPage.searchFor("something");
                searchResult.isAt();
                final FluentWebElement h1 = findFirst("h1");
                assertThat(h1.getText()).contains("Nothing found");

                // send a message and search for it
                final Channel channel = connectToGraylog2Gelf(tcpInputAddr);

                Map<String, Object> gelfMap = Maps.newHashMap();
                gelfMap.put("host", "testhost");
                gelfMap.put("short_message", "something");
                gelfMap.put("timestamp", System.currentTimeMillis() / 1000D);
                sendGelfMessage(channel, gelfMap);

                // this has a race condition, sleep long enough for the message to go through (yes this sucks)
                Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

                dashboardPage.go();
                final SearchPage searchPage = dashboardPage.searchFor("something");
                searchPage.isAt();
                final FluentWebElement headline = findFirst("h1");
                assertThat(headline.getText()).contains("Search results");
            }
        });
    }

}
