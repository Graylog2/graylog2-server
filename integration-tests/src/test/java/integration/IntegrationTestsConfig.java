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
package integration;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class IntegrationTestsConfig {
    private static final String GL_BASE_URI = System.getProperty("gl.baseuri", "http://localhost:12900");
    private static final String GL_PORT = System.getProperty("gl.port", "12900");
    private static final String GL_ADMIN_USER = System.getProperty("gl.admin_user", "admin");
    private static final String GL_ADMIN_PASSWORD = System.getProperty("gl.admin_password", "admin");
    private static final String MONGODB_HOST = System.getProperty("mongodb.host", "localhost");
    private static final String MONGODB_PORT = System.getProperty("mongodb.port", "27017");
    private static final String MONGODB_DATABASE = System.getProperty("mongodb.database", "graylog_test");
    private static final String ES_HOST = System.getProperty("es.host", "localhost");
    private static final String ES_CLUSTER_NAME = System.getProperty("es.cluster.name", "graylog_test");
    private static final String ES_PORT = System.getProperty("es.port", "9300");

    public static URL getGlServerURL() throws MalformedURLException, URISyntaxException {
        final URIBuilder result = new URIBuilder(GL_BASE_URI)
                .setPort(Integer.parseInt(GL_PORT))
                .setUserInfo(getGlAdminUser(), getGlAdminPassword());
        return result.build().toURL();
    }

    public static String getGlAdminUser() {
        return GL_ADMIN_USER;
    }

    public static String getGlAdminPassword() {
        return GL_ADMIN_PASSWORD;
    }

    public static String getMongodbHost() {
        return MONGODB_HOST;
    }

    public static int getMongodbPort() {
        return Integer.parseInt(MONGODB_PORT);
    }

    public static String getMongodbDatabase() {
        return MONGODB_DATABASE;
    }

    public static String getEsHost() {
        return ES_HOST;
    }

    public static int getEsPort() {
        return Integer.parseInt(ES_PORT);
    }

    public static String getEsClusterName() {
        return ES_CLUSTER_NAME;
    }
}
