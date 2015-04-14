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
package org.graylog2.restclient.models;

import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ExtractorTest {
    private Extractor extractor;
    @Mock
    private User mockUser;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        extractor = new Extractor(Extractor.CursorStrategy.COPY, "title", "source", "target",
                Extractor.Type.COPY_INPUT, mockUser, Extractor.ConditionType.NONE, "");
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Extractor type must not be null\\.$")
    public void loadConfigFromImportThrowsNPEIfTypeIsNull() throws Exception {
        extractor.loadConfigFromImport(null, Collections.<String, Object>emptyMap());
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "^Extractor configuration must not be null\\.$")
    public void loadConfigFromImportThrowsNPEIfConfigIsNull() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.COPY_INPUT, null);
    }

    @Test
    public void loadConfigFromImportSucceedsWithEmptyConfigForCopyInputExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.COPY_INPUT, Collections.<String, Object>emptyMap());
        assertTrue(extractor.getExtractorConfig().isEmpty());
    }

    @Test
    public void loadConfigFromImportSucceedsWithValidConfigForGrokExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.GROK, Collections.<String, Object>singletonMap("grok_pattern", "pattern"));
        assertEquals(extractor.getExtractorConfig().get("grok_pattern"), "pattern");
    }

    @Test
    public void loadConfigFromImportSucceedsWithValidConfigForRegexExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.REGEX, Collections.<String, Object>singletonMap("regex_value", "pattern"));
        assertEquals(extractor.getExtractorConfig().get("regex_value"), "pattern");
    }

    @Test
    public void loadConfigFromImportSucceedsWithValidConfigForSplitAndIndexExtractor() throws Exception {
        extractor.loadConfigFromImport(
                Extractor.Type.SPLIT_AND_INDEX,
                ImmutableMap.<String, Object>of(
                        "split_by", ",",
                        "index", "0"
                ));
        assertEquals(extractor.getExtractorConfig().get("split_by"), ",");
        assertEquals(extractor.getExtractorConfig().get("index"), 0);
    }

    @Test
    public void loadConfigFromImportSucceedsWithValidConfigForSubstringExtractor() throws Exception {
        extractor.loadConfigFromImport(
                Extractor.Type.SUBSTRING,
                ImmutableMap.<String, Object>of(
                        "begin_index", "0",
                        "end_index", "1"
                ));
        assertEquals(extractor.getExtractorConfig().get("begin_index"), 0);
        assertEquals(extractor.getExtractorConfig().get("end_index"), 1);
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "^Missing extractor config:.*")
    public void loadConfigFromImportFailsWithEmptyConfigForGrokExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.GROK, Collections.<String, Object>emptyMap());
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "^Missing extractor config:.*")
    public void loadConfigFromImportFailsWithEmptyConfigForRegexExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.REGEX, Collections.<String, Object>emptyMap());
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "^Missing extractor config:.*")
    public void loadConfigFromImportFailsWithEmptyConfigForSplitAndIndexExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.SPLIT_AND_INDEX, Collections.<String, Object>emptyMap());
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "^Missing extractor config:.*")
    public void loadConfigFromImportFailsWithEmptyConfigForSubstringExtractor() throws Exception {
        extractor.loadConfigFromImport(Extractor.Type.SUBSTRING, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConvertersFromFormDoesNothingIfFormIsNull() throws Exception {
        extractor.loadConvertersFromForm(null);
        assertTrue(extractor.getConverters().isEmpty());
    }

    @Test
    public void loadConvertersFromFormDoesNothingIfFormIsEmpty() throws Exception {
        extractor.loadConvertersFromForm(Collections.<String, String[]>emptyMap());
        assertTrue(extractor.getConverters().isEmpty());
    }

    @Test
    public void testLoadConvertersFromForm() throws Exception {
        extractor.loadConvertersFromForm(ImmutableMap.of("converter_lowercase", new String[]{"enabled"}));
        assertEquals(extractor.getConverters().size(), 1);

        Converter converter = extractor.getConverters().get(0);
        assertEquals(converter.getType(), "lowercase");
    }

    @Test
    public void loadConvertersFromImportDoesNothingIfImportsIsNull() throws Exception {
        extractor.loadConvertersFromImport(null);
        assertTrue(extractor.getConverters().isEmpty());
    }

    @Test
    public void loadConvertersFromImportDoesNothingIfImportsIsEmpty() throws Exception {
        extractor.loadConvertersFromImport(Collections.<Map<String, Object>>emptyList());
        assertTrue(extractor.getConverters().isEmpty());
    }

    @Test
    public void testLoadConvertersFromImport() throws Exception {
        extractor.loadConvertersFromImport(Collections.<Map<String, Object>>singletonList(ImmutableMap.of(
                "type", "lowercase",
                "config", Collections.<String, Object>emptyMap()
        )));
        assertEquals(extractor.getConverters().size(), 1);

        Converter converter = extractor.getConverters().get(0);
        assertEquals(converter.getType(), "lowercase");
    }
}