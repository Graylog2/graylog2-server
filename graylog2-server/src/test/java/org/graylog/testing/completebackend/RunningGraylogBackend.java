package org.graylog.testing.completebackend;

import org.apache.commons.lang3.NotImplementedException;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.testcontainers.containers.Network;

import java.lang.reflect.Constructor;

public class RunningGraylogBackend implements GraylogBackend {
    private final SearchServerInstance searchServerInstance;

    public RunningGraylogBackend() {
        try {
            Class<?> clazz = Class.forName("org.graylog.storage.elasticsearch7.testing.RunningElasticsearchInstanceES7");
            Constructor constructor = clazz.getConstructor();
            this.searchServerInstance = (SearchServerInstance) constructor.newInstance();
        } catch (Exception ex) {
            throw new NotImplementedException("Could not create Search instance.", ex);
        }
    }

    public static GraylogBackend createStarted() {
        return new RunningGraylogBackend();
    }

    @Override
    public String uri() {
        return "http://localhost";
    }

    @Override
    public int apiPort() {
        return 9000;
    }

    @Override
    public SearchServerInstance searchServerInstance() {
        return this.searchServerInstance;
    }

    @Override
    public int mappedPortFor(int originalPort) {
        return originalPort;
    }

    @Override
    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {
        throw new NotImplementedException("Feature needs implementation...");
    }

    @Override
    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        this.searchServerInstance.importFixtureResource(resourcePath, testClass);
    }

    @Override
    public Network network() {
        throw new NotImplementedException("Feature not implemented on Running backends (no container)");
    }
}
