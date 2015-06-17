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
package integration.util.graylog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class GraylogControl {
    private URL url;
    private static final int HTTP_TIMEOUT = 1000;

    public GraylogControl() {
        this.url = getDefaultServerUrl();
    }

    public GraylogControl(URL url) {
        this.url = url;
    }

    public void startServer() {
        System.out.println("Starting Graylog server...");
        try {
            URL startupUrl = new URL("http", url.getHost(), 5000, "/sv/up/graylog-server");

            doRequest(startupUrl);
            waitForStartup();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        System.out.println("Stoping Graylog server...");
        try {
            URL shutdownUrl = new URL("http", url.getHost(), 5000, "/sv/down/graylog-server");

            doRequest(shutdownUrl);
            waitForShutdown();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }
    public void restartServer() {
        System.out.println("Reestarting Graylog server...");
        stopServer();
        startServer();
    }

    public void waitForStartup() {
        try {
            URL apiCheckUrl = new URL(url, "/system/stats");
            URL svCheckUrl = new URL("http", url.getHost(), 5000, "/sv/status/graylog-server");

            Boolean serverRunning = false;
            while (!serverRunning) {
                Boolean apiAvailable = doRequest(apiCheckUrl);
                Boolean svStatusUp = doRequest(svCheckUrl);
                serverRunning = apiAvailable && svStatusUp;

                sleep(1000);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void waitForShutdown() {
        try {
            URL apiCheckUrl = new URL(url, "/system/stats");
            URL svCheckUrl = new URL("http", url.getHost(), 5000, "/sv/status/graylog-server");

            Boolean serverRunning = true;
            while (serverRunning) {
                Boolean apiAvailable = doRequest(apiCheckUrl);
                Boolean svStatusUp = doRequest(svCheckUrl);
                serverRunning = apiAvailable && svStatusUp;

                sleep(1000);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private boolean doRequest(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setRequestMethod("GET");

            if (url.getUserInfo() != null) {
                String encodedUserInfo = Base64.encodeBase64String(url.getUserInfo().getBytes());
                connection.setRequestProperty("Authorization", "Basic " + encodedUserInfo);
            }

            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            return false;
        }
    }

    public String getNodeId() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url, "/system").openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setRequestMethod("GET");

            if (url.getUserInfo() != null) {
                String encodedUserInfo = Base64.encodeBase64String(url.getUserInfo().getBytes());
                connection.setRequestProperty("Authorization", "Basic " + encodedUserInfo);
            }

            InputStream inputStream = connection.getInputStream();
            JsonNode json = mapper.readTree(inputStream);
            return json.get("server_id").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "00000000-0000-0000-0000-000000000000";
    }

    public URL getUrl() {
        return url;
    }

    private URL getDefaultServerUrl() {
        try {
            final URIBuilder uriBuilder = new URIBuilder(System.getProperty("gl2.baseuri", "http://localhost"));
            uriBuilder.setPort(Integer.parseInt(System.getProperty("gl2.port", "12900")));
            uriBuilder.setUserInfo(System.getProperty("gl2.admin_user", "admin"), System.getProperty("gl2.admin_password", "admin"));
            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Invalid URI given. Skipping integration tests.");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // interrupted
        }
    }
}
