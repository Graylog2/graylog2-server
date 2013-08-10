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

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegexExtractorTest {

    @Test
    public void testBasicExtraction() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+)"));
        x.run(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "our_result", config("id:(\\d+)"));
        x.run(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatch() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.COPY, "somefield", "our_result", config("nothing:(\\d+)"));
        x.run(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testBasicExtractionDoesNotFailOnNonMatchWithCutStrategy() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001");

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "our_result", config("nothing:(\\d+)"));
        x.run(msg);

        assertNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001", msg.getField("somefield"));
    }

    @Test
    public void testExtractsFirstMatcherGroupWhenProvidedWithSeveral() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", "<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001 lolwut");

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.COPY, "somefield", "our_result", config("id:(\\d+).*(lolwut)"));
        x.run(msg);

        assertNotNull(msg.getField("our_result"));
        assertEquals("<10> 07 Aug 2013 somesubsystem: this is my message for username9001 id:9001 lolwut", msg.getField("somefield"));
        assertEquals("9001", msg.getField("our_result"));
    }

    @Test
    public void testDoesNotFailOnNonExistentSourceField() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "LOLIDONTEXIST", "our_result", config("id:(\\d+)"));
        x.run(msg);
    }

    @Test
    public void testDoesNotFailOnSourceFieldThatIsNotOfTypeString() throws Exception {
        Message msg = new Message("The short message", "TestUnit", Tools.getUTCTimestampWithMilliseconds());

        msg.addField("somefield", 9001);

        RegexExtractor x = new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "our_result", config("id:(\\d+)"));
        x.run(msg);
    }

    @Test(expected = Extractor.ReservedFieldException.class)
    public void testDoesNotRunAgainstReservedFields() throws Exception {
        new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "source", config("id:(\\d+)"));
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullConfigMap() throws Exception {
        new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "somefield", null);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnNullRegexValue() throws Exception {
        new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "somefield", config(null));
    }

    @Test(expected = ConfigurationException.class)
    public void testDoesNotInitializeOnEmptyRegexValue() throws Exception {
        new RegexExtractor("foo", Extractor.CursorStrategy.CUT, "somefield", "somefield", config(""));
    }

    public static Map<String, Object> config(final String regex) {
        return new HashMap<String, Object>() {{
            put("regex_value", regex);
        }};
    }
}
