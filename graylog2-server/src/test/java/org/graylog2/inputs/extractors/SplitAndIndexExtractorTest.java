/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.extractors;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SplitAndIndexExtractorTest extends AbstractExtractorTest {
    @Test
    public void testBasicExtraction() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithSpecialRegexChar() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "foo.bar.baz");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(".", 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("foo.bar.baz", msg.getField("somefield"));
        assertEquals("bar", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug  somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategyCanOverwriteSameField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "message", config(" ", 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertEquals("short", msg.getField("message"));
    }

    @Test
    public void testBasicExtractionWithCutStrategyAtEndOfString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 12), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001", msg.getField("somefield"));
        assertEquals("id:9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnTooHighIndex() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonExistentSplitChar() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("_", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnTooHighIndexWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonExistentSplitCharWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("_", 9001), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionWorksWithMultipleSplitChars() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10>__07__Aug__2013__somesubsystem:__this__is__my__message__for__username9001__id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("__", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10>__07__Aug__2013__somesubsystem:__this__is__my__message__for__username9001__id:9001", msg.getField("somefield"));
        assertEquals("2013", msg.getField("our_result"));
    }

    @Test
    public void testDoesNotFailOnNonExistentSourceField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "LOLIDONTEXIST", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test
    public void testDoesNotFailOnSourceFieldThatIsNotOfTypeString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", 9001);

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(" ", 4), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullConfigMap() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", null, "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullSplitChar() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullTargetIndex() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config("x", null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullNullNullNullSupernull() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null, null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnWrongSplitCharType() throws Exception {
        new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(1, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
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
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 3), "foo", noConverters(), Extractor.ConditionType.REGEX, "^XXX");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotRunWhenStringConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(" ", 3), "foo", noConverters(), Extractor.ConditionType.STRING, "FOOBAR");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotCutFromStandardFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SplitAndIndexExtractor x = new SplitAndIndexExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "our_result", config(" ", 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        // Would be cut to "short message" if cutting from standard field was allowed.
        assertEquals("The short message", msg.getField("message"));
    }

    @Test
    public void testCutChecksBounds() throws Exception {
        String result = SplitAndIndexExtractor.cut("foobar", " ", 1);

        assertNull(result);
    }

    @Test
    public void testCutWorksWithNull() throws Exception {
        assertNull(SplitAndIndexExtractor.cut(null, " ", 1));
        assertNull(SplitAndIndexExtractor.cut("foobar", null, 1));
        assertNull(SplitAndIndexExtractor.cut("foobar", " ", -1));
    }

    @Test
    public void testCutReturnsCorrectPart() throws Exception {
        String result = SplitAndIndexExtractor.cut("foobar foobaz quux", " ", 2);

        assertEquals("quux", result);
    }

    public static Map<String, Object> config(final Object splitChar, final Object targetIndex) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("index", targetIndex);
        map.put("split_by", splitChar);
        return map;
    }
}
