package org.graylog.plugins.views.search.export;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

public interface ExportFormat {
    MediaType mimeType();

    default Optional<String> hasError() {
        return Optional.empty();
    }
}
