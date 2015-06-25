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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class ServerHelper {
    private URL url;
    private static final int HTTP_TIMEOUT = 1000;

    public ServerHelper() {
        this.url = getDefaultServerUrl();
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
}
