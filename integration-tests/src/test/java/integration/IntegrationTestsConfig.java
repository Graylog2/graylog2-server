package integration;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class IntegrationTestsConfig {
    private static final String glBaseURi = System.getProperty("gl.baseuri", "http://localhost:12900");
    private static final String glPort = System.getProperty("gl.port", "12900");
    private static final String glAdminUser = System.getProperty("gl.admin_user", "admin");
    private static final String glAdminPassword = System.getProperty("gl.admin_password", "admin");
    private static final String mongodbHost = System.getProperty("mongodb.host", "localhost");
    private static final String mongodbPort = System.getProperty("mongodb.port", "27017");
    private static final String mongodbDatabase = System.getProperty("mongodb.database", "graylog2");
    private static final String esHost = System.getProperty("es.host", "localhost");
    private static final String esClusterName = System.getProperty("es.cluster.name", "graylog");
    private static final String esPort = System.getProperty("es.port", "9300");

    public static URL getGlServerURL() throws MalformedURLException, URISyntaxException {
        final URIBuilder result = new URIBuilder(glBaseURi)
                .setPort(Integer.parseInt(glPort))
                .setUserInfo(getGlAdminUser(), getGlAdminPassword());
        return result.build().toURL();
    }

    public static String getGlAdminUser() {
        return glAdminUser;
    }

    public static String getGlAdminPassword() {
        return glAdminPassword;
    }

    public static String getMongodbHost() {
        return mongodbHost;
    }

    public static int getMongodbPort() {
        return Integer.parseInt(mongodbPort);
    }

    public static String getMongodbDatabase() {
        return mongodbDatabase;
    }

    public static String getEsHost() {
        return esHost;
    }

    public static int getEsPort() {
        return Integer.parseInt(esPort);
    }

    public static String getEsClusterName() {
        return esClusterName;
    }
}
