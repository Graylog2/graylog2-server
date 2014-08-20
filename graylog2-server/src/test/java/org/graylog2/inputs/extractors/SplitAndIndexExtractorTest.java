/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.extractors;

import com.google.common.collect.Lists;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SplitAndIndexExtractorTest extends AbstractExtractorTest {
    @Test
    public void testBasicExtraction() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithSpecialRegexChar() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "foo.bar.baz");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(".", 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("foo.bar.baz", msg.getField("somefield"));
        assertEquals("bar", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug  somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategyCanOverwriteSameField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "message", config(" ", 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertEquals("short", msg.getField("message"));
    }
    @Test
    public void testBasicExtractionWithCutStrategyAtEndOfString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 12), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001", msg.getField("somefield"));
        assertEquals("id:9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnTooHighIndex() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonExistentSplitChar() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("_", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnTooHighIndexWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonExistentSplitCharWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("_", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionWorksWithMultipleSplitChars() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10>__07__Aug__2013__somesubsystem:__this__is__my__message__for__username9001__id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("__", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10>__07__Aug__2013__somesubsystem:__this__is__my__message__for__username9001__id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testDoesNotFailOnNonExistentSourceField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "LOLIDONTEXIST", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test
    public void testDoesNotFailOnSourceFieldThatIsNotOfTypeString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", 9001);

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnNullConfigMap() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", null, "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnNullSplitChar() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
         public void testDoesNotInitializeOnNullTargetIndex() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config("x", null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnNullNullNullNullSupernull() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null, null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnWrongSplitCharType() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(1, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnWrongTargetIndex() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config("x", "foo"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test
    public void testCutIndices() throws Exception {
        int[] result = SplitAndIndexExtractor.getCutIndices("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", " ", 3);

        assertEquals(12, result[0]);
        assertEquals(16, result[1]);
    }

    @Test
    public void testCutIndicesWithLastToken() throws Exception {
        String s = "<10> 07 Aug 2013 somesubsystem";
        int[] result = SplitAndIndexExtractor.getCutIndices(s, " ", 4);

        assertEquals(17, result[0]);
        assertEquals(s.length(), result[1]);
    }

    @Test
    public void testCutIndicesWithFirstToken() throws Exception {
        String s = "<10> 07 Aug 2013 somesubsystem";
        int[] result = SplitAndIndexExtractor.getCutIndices(s, " ", 0);

        assertEquals(0, result[0]);
        assertEquals(4, result[1]);
    }

    @Test
    public void testCutIndicesReturnsZeroesOnInvalidBoundaries() throws Exception {
        int[] result = SplitAndIndexExtractor.getCutIndices("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", " ", 9001);

        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
    }

    @Test
    public void testDoesNotRunWhenRegexConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 3), "foo", noConverters(), Extractor.ConditionType.REGEX, "^XXX");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotRunWhenStringConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 3), "foo", noConverters(), Extractor.ConditionType.STRING, "FOOBAR");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotCutFromStandardFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "our_result", config(" ", 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        // Would be cut to "short message" if cutting from standard field was allowed.
        assertEquals("The short message", msg.getField("message"));
    }

    public static Map<String, Object> config(final Object splitChar, final Object targetIndex) {
        return new HashMap<String, Object>() {{
            put("index", targetIndex);
            put("split_by", splitChar);
        }};
    }

    public static List<Converter> noConverters() {
        return Lists.newArrayList();
    }

}
