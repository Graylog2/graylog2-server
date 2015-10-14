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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ExtractorTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Extractor extractor;
    @Mock
    private User mockUser;

    @Before
    public void setUp() {
        extractor = new Extractor(Extractor.CursorStrategy.COPY, "title", "source", "target",
                Extractor.Type.COPY_INPUT, mockUser, Extractor.ConditionType.NONE, "");
    }

    @Test
    public void loadConfigFromImportThrowsNPEIfTypeIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Extractor type must not be null");

        extractor.loadConfigFromImport(null, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConfigFromImportThrowsNPEIfConfigIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Extractor configuration must not be null");

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
    public void loadConfigFromImportSucceedsWithValidConfigForRegexReplaceExtractor() throws Exception {
        extractor.loadConfigFromImport(
                Extractor.Type.REGEX_REPLACE,
                ImmutableMap.<String, Object>of(
                        "regex", "foobar",
                        "replacement", "***"
                ));
        assertEquals(extractor.getExtractorConfig().get("regex"), "foobar");
        assertEquals(extractor.getExtractorConfig().get("replacement"), "***");
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

    @Test
    public void loadConfigFromImportFailsWithEmptyConfigForGrokExtractor() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Missing extractor config:");

        extractor.loadConfigFromImport(Extractor.Type.GROK, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConfigFromImportFailsWithEmptyConfigForRegexExtractor() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Missing extractor config:");

        extractor.loadConfigFromImport(Extractor.Type.REGEX, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConfigFromImportFailsWithEmptyConfigForSplitAndIndexExtractor() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Missing extractor config:");

        extractor.loadConfigFromImport(Extractor.Type.SPLIT_AND_INDEX, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConfigFromImportFailsWithEmptyConfigForSubstringExtractor() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Missing extractor config:");

        extractor.loadConfigFromImport(Extractor.Type.SUBSTRING, Collections.<String, Object>emptyMap());
    }

    @Test
    public void loadConfigFromImportFailsWithEmptyConfigForRegexReplaceExtractor() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Missing extractor config:");

        extractor.loadConfigFromImport(Extractor.Type.REGEX_REPLACE, Collections.<String, Object>emptyMap());
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
