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

public class SubstringExtractorTest extends AbstractExtractorTest {
    @Test
    public void testBasicExtraction() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(17, 30), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("somesubsystem", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(17, 30), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 : this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("somesubsystem", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategyCanOverwriteSameField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "message", config(4, 17), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertEquals("short message", msg.getField("message"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatch() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(100, 200), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatchWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(100, 200), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotFailOnNonExistentSourceField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "LOLIDONTEXIST", "our_result", config(0, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test
    public void testDoesNotFailOnSourceFieldThatIsNotOfTypeString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", 9001);

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(0, 1), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);
    }

    @Test
    public void testBasicExtractionWithCutStrategyDoesNotLeaveEmptyFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config(0, 75), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("fullyCutByExtractor", msg.getField("somefield"));
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullConfigMap() throws Exception {
        new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", null, "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullStartValue() throws Exception {
        new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null, 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullEndValue() throws Exception {
        new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(1, null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnStringStartValue() throws Exception {
        new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config("1", 2), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnStringEndValue() throws Exception {
        new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(1, "2"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test
    public void testDoesNotRunWhenRegexConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(0, 3), "foo", noConverters(), Extractor.ConditionType.REGEX, "^XXX");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotRunWhenStringConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config(0, 3), "foo", noConverters(), Extractor.ConditionType.STRING, "FOOBAR");
        x.runExtractor(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotCutFromStandardFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.nowUTC());

        SubstringExtractor x = new SubstringExtractor(metricRegistry, "foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "our_result", config(0, 3), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(msg);

        // Would be cut to "short message" if cutting from standard field was allowed.
        assertEquals("The short message", msg.getField("message"));
    }

    public static Map<String, Object> config(final Object start, final Object end) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("begin_index", start);
        map.put("end_index", end);
        return map;
    }
}
