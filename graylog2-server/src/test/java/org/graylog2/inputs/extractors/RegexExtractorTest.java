/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.inputs.extractors;

import com.google.common.collect.Lists;
import org.graylog2.ConfigurationException;
import org.graylog2.GraylogServerStub;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegexExtractorTest {

    @Test
    public void testBasicExtraction() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategyCanOverwriteSameField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "message", config("The (.+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertEquals("short message", msg.getField("message"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatch() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("nothing:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatchWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("nothing:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testExtractsFirstMatcherGroupWhenProvidedWithSeveral() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001 lolwut");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+).*(lolwut)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001 lolwut", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testDoesNotFailOnNonExistentSourceField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "LOLIDONTEXIST", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);
    }

    @Test
    public void testDoesNotFailOnSourceFieldThatIsNotOfTypeString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", 9001);

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);
    }

    @Test
    public void testBasicExtractionWithCutStrategyDoesNotLeaveEmptyFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "our_result", config("(.*)"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("fullyCutByExtractor", msg.getField("somefield"));
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnNullConfigMap() throws Exception {
        new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", null, "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnNullRegexValue() throws Exception {
        new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testDoesNotInitializeOnEmptyRegexValue() throws Exception {
        new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "somefield", "somefield", config(""), "foo", noConverters(), Extractor.ConditionType.NONE, null);
    }

    @Test
    public void testDoesNotRunWhenRegexConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.REGEX, "^XXX");
        x.runExtractor(new GraylogServerStub(), msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotRunWhenStringConditionFails() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+)"), "foo", noConverters(), Extractor.ConditionType.STRING, "FOOBAR");
        x.runExtractor(new GraylogServerStub(), msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testDoesNotCutFromStandardFields() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.iso8601());

        RegexExtractor x = new RegexExtractor("foo", "foo", 0, Extractor.CursorStrategy.CUT, "message", "our_result", config("^(The).+"), "foo", noConverters(), Extractor.ConditionType.NONE, null);
        x.runExtractor(new GraylogServerStub(), msg);

        // Would be cut to "short message" if cutting from standard field was allowed.
        assertEquals("The short message", msg.getField("message"));
    }

    public static Map<String, Object> config(final String regex) {
        return new HashMap<String, Object>() {{
            put("regex_value", regex);
        }};
    }

    public static List<Converter> noConverters() {
        return Lists.newArrayList();
    }
}
