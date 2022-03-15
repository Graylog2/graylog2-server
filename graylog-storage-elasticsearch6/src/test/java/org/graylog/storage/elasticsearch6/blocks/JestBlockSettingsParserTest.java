package org.graylog.storage.elasticsearch6.blocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestResult;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JestBlockSettingsParserTest {

    private JestBlockSettingsParser toTest;
    private JestResult jestResult;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        toTest = new JestBlockSettingsParser();
        jestResult = mock(JestResult.class);
    }

    @Test
    public void noBlockedIndicesIdentifiedIfEmptyResponseParsed() throws Exception {
        when(jestResult.getJsonObject()).thenReturn(mapper.readTree("{}"));
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, Collections.singletonList("graylog_0"));
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void noBlockedIndicesIdentifiedIfEmptyListOfIndicesProvided() {
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, Collections.emptyList());
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void noBlockedIndicesIdentifiedIfNullListOfIndicesProvided() {
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, null);
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void noBlockedIndicesIdentifiedIfFalseBlockPresent() throws Exception {
        String json = "{\n" +
                "  \"graylog_0\": {\n" +
                "    \"settings\": {\n" +
                "      \"index\": {\n" +
                "        \"blocks\": {\n" +
                "          \"read_only\": \"false\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }  \n" +
                "}";
        when(jestResult.getJsonObject()).thenReturn(mapper.readTree(json));
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, Collections.singletonList("graylog_0"));
        assertNotNull(indicesBlockStatus);
        assertEquals(0, indicesBlockStatus.countBlockedIndices());
    }

    @Test
    public void blockedIndicesIdentifiedIfFalseBlockPresent() throws Exception {
        String json = "{\n" +
                "  \"graylog_0\": {\n" +
                "    \"settings\": {\n" +
                "      \"index\": {\n" +
                "        \"blocks\": {\n" +
                "          \"read_only\": \"true\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }  \n" +
                "}";
        when(jestResult.getJsonObject()).thenReturn(mapper.readTree(json));
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, Collections.singletonList("graylog_0"));
        assertNotNull(indicesBlockStatus);
        final Set<String> blockedIndices = indicesBlockStatus.getBlockedIndices();
        assertEquals(1, indicesBlockStatus.countBlockedIndices());
        assertTrue(blockedIndices.contains("graylog_0"));
        Collection<String> indexBlocks = indicesBlockStatus.getIndexBlocks("graylog_0");
        assertEquals(1, indexBlocks.size());
        assertTrue(indexBlocks.contains("index.blocks.read_only"));
    }

    @Test
    public void blockedIndicesIdentifiedAmongUnblockedOnes() throws Exception {
        String json = "{\n" +
                "  \"graylog_0\": {\n" +
                "    \"settings\": {\n" +
                "      \"index\": {\n" +
                "        \"blocks\": {\n" +
                "          \"read_only\": \"true\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },  \n" +
                "  \"unblocked1\": {\n" +
                "    \"settings\": {\n" +
                "      \"index\": {\n" +
                "        \"blocks\": {\n" +
                "          \"read_only\": \"false\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },  \n" +
                "  \"unblocked2\": {\n" +
                "    \"settings\": {\n" +
                "      \"index\": {\n" +
                "        \"blocks\": {\n" +
                "          \"read_only\": false\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }  \n" +

                "}";
        when(jestResult.getJsonObject()).thenReturn(mapper.readTree(json));
        final IndicesBlockStatus indicesBlockStatus = toTest.parseBlockSettings(jestResult, Arrays.asList("graylog_0", "unblocked1", "unblocked2"));
        assertNotNull(indicesBlockStatus);
        final Set<String> blockedIndices = indicesBlockStatus.getBlockedIndices();
        assertEquals(1, indicesBlockStatus.countBlockedIndices());
        assertTrue(blockedIndices.contains("graylog_0"));
        Collection<String> indexBlocks = indicesBlockStatus.getIndexBlocks("graylog_0");
        assertEquals(1, indexBlocks.size());
        assertTrue(indexBlocks.contains("index.blocks.read_only"));
    }
}
