package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class NodeAdapterES7Test {

    private NodeAdapterES7 toTest;
    private PlainJsonApi jsonApiMock;
    private Request request;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jsonApiMock = mock(PlainJsonApi.class);
        toTest = new NodeAdapterES7(jsonApiMock);
        request = new Request("GET", "/?filter_path=version.number,version.distribution");
    }

    @Test
    void testElasticsearchVersionFetching() throws IOException {
        mockResponse("{\"version\" : " +
                " {" +
                "    \"number\" : \"7.10.2\"" +
                " }" +
                "}");

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.valueOf("7.10.2")));

    }

    @Test
    void testOpensearchVersionFetching() throws IOException {
        mockResponse("{\"version\" : " +
                "  {" +
                "    \"distribution\" : \"opensearch\"," +
                "    \"number\" : \"1.3.1\"" +
                "  }" +
                "}");

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.3.1")));

    }

    private void mockResponse(final String jsonResponseWithVersion) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(jsonResponseWithVersion);
        doReturn(jsonNode).when(jsonApiMock).perform(eq(request), anyString());
    }
}
