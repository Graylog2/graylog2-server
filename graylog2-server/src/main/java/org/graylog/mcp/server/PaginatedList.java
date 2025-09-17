package org.graylog.mcp.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;

import java.util.List;

// TODO pagination is really awkward to apply to our internal paginated services at the moment.
// as there seems to be little support for pagination in general in MCP clients, we'll skip it for now.
public record PaginatedList<T>(@Nonnull List<T> list, @Nullable Cursor cursor) {
    public record Cursor(String nextCursor) {}
}
