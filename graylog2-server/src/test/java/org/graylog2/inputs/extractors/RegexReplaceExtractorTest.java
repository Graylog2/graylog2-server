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
package org.graylog2.inputs.extractors;

import org.elasticsearch.common.collect.ImmutableMap;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexReplaceExtractorTest extends AbstractExtractorTest {
    @Test(expected = ConfigurationException.class)
    public void testConstructorWithMissingRegex() throws Exception {
        new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of(),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
    }

    @Test(expected = ConfigurationException.class)
    public void testConstructorWithNonStringRegex() throws Exception {
        new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", 0L),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
    }

    @Test(expected = ConfigurationException.class)
    public void testConstructorWithNonStringReplacement() throws Exception {
        new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "NO-MATCH", "replacement", 0L),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
    }

    @Test
    public void testReplacementWithNoMatchAndDefaultReplacement() throws Exception {
        final Message message = new Message("Test", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "NO-MATCH"),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);

        assertThat(message.getMessage()).isEqualTo("Test");
    }

    @Test
    public void testReplacementWithOnePlaceholder() throws Exception {
        final Message message = new Message("Test Foobar", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "Test (\\w+)"),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);

        assertThat(message.getMessage()).isEqualTo("Foobar");
    }

    @Test(expected = RuntimeException.class)
    public void testReplacementWithTooManyPlaceholders() throws Exception {
        final Message message = new Message("Foobar 123", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "Foobar (\\d+)", "replacement", "$1 $2"),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);
    }

    @Test
    public void testReplacementWithCustomReplacement() throws Exception {
        final Message message = new Message("Foobar 123", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "(Foobar) (\\d+)", "replacement", "$2/$1"),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);

        assertThat(message.getMessage()).isEqualTo("123/Foobar");
    }

    @Test
    public void testReplacementWithReplaceAll() throws Exception {
        final Message message = new Message("Foobar 123 Foobaz 456", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "(\\w+) (\\d+)", "replacement", "$2/$1", "replace_all", true),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);

        assertThat(message.getMessage()).isEqualTo("123/Foobar 456/Foobaz");
    }

    @Test
    public void testReplacementWithoutReplaceAll() throws Exception {
        final Message message = new Message("Foobar 123 Foobaz 456", "source", Tools.iso8601());
        final RegexReplaceExtractor extractor = new RegexReplaceExtractor(
                metricRegistry,
                "id",
                "title",
                0L,
                Extractor.CursorStrategy.COPY,
                "message",
                "message",
                ImmutableMap.<String, Object>of("regex", "(\\w+) (\\d+)", "replacement", "$2/$1", "replace_all", false),
                "user",
                Collections.<Converter>emptyList(),
                Extractor.ConditionType.NONE,
                null);
        extractor.runExtractor(message);

        assertThat(message.getMessage()).isEqualTo("123/Foobar Foobaz 456");
    }
}