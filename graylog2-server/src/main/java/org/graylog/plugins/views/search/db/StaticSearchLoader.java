package org.graylog.plugins.views.search.db;

import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.IOException;
import java.net.URL;

/**
 * Provides ability to load search records from resources during server startup.
 */
public class StaticSearchLoader {
    public static SearchDTO loadSearchResource(URL fileUrl) {
        try {
            return new ObjectMapperProvider().get().readValue(fileUrl, SearchDTO.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load search.", e);
        }
    }
}
