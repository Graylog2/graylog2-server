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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.ConfigurationException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GrokExtractorTest {

    private Set<GrokPattern> patternSet;

    @Before
    public void setUp() throws Exception {
        patternSet = Sets.newHashSet();

        final GrokPattern baseNum = new GrokPattern();
        baseNum.name = "BASE10NUM";
        baseNum.pattern = "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))";

        final GrokPattern number = new GrokPattern();
        number.name = "NUMBER";
        number.pattern = "(?:%{BASE10NUM:UNWANTED})";

        patternSet.add(baseNum);
        patternSet.add(number);
    }

    @Test
    public void testDatatypeExtraction() {
        final GrokExtractor extractor = makeExtractor("%{NUMBER:number;int}");

        final Extractor.Result[] results = extractor.run("199999");
        assertEquals("NUMBER is marked as UNWANTED and does not generate a field", 1, results.length);
        assertEquals(Integer.class, results[0].getValue().getClass());
        assertEquals(199999, results[0].getValue());
    }

    private GrokExtractor makeExtractor(String pattern) {
        Map<String, Object> config = Maps.newHashMap();
        config.put("grok_pattern", pattern);

        try {
            return new GrokExtractor(new LocalMetricRegistry(),
                                     patternSet,
                                     "id",
                                     "title",
                                     0,
                                     Extractor.CursorStrategy.COPY,
                                     "message",
                                     "message",
                                     config,
                                     "admin",
                                     Lists.<Converter>newArrayList(),
                                     Extractor.ConditionType.NONE,
                                     null);
        } catch (Extractor.ReservedFieldException | ConfigurationException e) {
            fail("Test setup is wrong: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}