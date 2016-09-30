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
import com.google.common.collect.Sets;
import org.graylog2.ConfigurationException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GrokExtractorTest {

    private Set<GrokPattern> patternSet;

    @Before
    public void setUp() throws Exception {
        patternSet = Sets.newHashSet();

        final GrokPattern baseNum = GrokPattern.create("BASE10NUM", "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))");
        final GrokPattern number = GrokPattern.create("NUMBER", "(?:%{BASE10NUM:UNWANTED})");
        final GrokPattern data = GrokPattern.create("GREEDY", ".*");

        patternSet.add(baseNum);
        patternSet.add(number);
        patternSet.add(data);
    }

    @Test
    public void testDatatypeExtraction() {
        final GrokExtractor extractor = makeExtractor("%{NUMBER:number;int}");

        final Extractor.Result[] results = extractor.run("199999");
        assertEquals("NUMBER is marked as UNWANTED and does not generate a field", 1, results.length);
        assertEquals(Integer.class, results[0].getValue().getClass());
        assertEquals(199999, results[0].getValue());
    }

    @Test
    public void testDateExtraction() {
        final GrokExtractor extractor = makeExtractor("%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss.SSSX}");
        final Extractor.Result[] results = extractor.run("2015-07-31T10:05:36.773Z");
        assertEquals("ISO date is parsed", 1, results.length);
        Object value = results[0].getValue();
        assertTrue(value instanceof Date);
        DateTime date = new DateTime(value, DateTimeZone.UTC);

        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthOfYear());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(10, date.getHourOfDay());
        assertEquals(5, date.getMinuteOfHour());
        assertEquals(36, date.getSecondOfMinute());
        assertEquals(773, date.getMillisOfSecond());
    }

    @Test
    public void testNamedCapturesOnly() throws Exception {
        final Map<String, Object> config = new HashMap<>();

        final GrokPattern mynumber = GrokPattern.create("MYNUMBER", "(?:%{BASE10NUM})");

        patternSet.add(mynumber);

        config.put("named_captures_only", true);
        final GrokExtractor extractor1 = makeExtractor("%{MYNUMBER:num}", config);

        config.put("named_captures_only", true);
        final GrokExtractor extractor2 = makeExtractor("%{MYNUMBER:num;int}", config);

        config.put("named_captures_only", false);
        final GrokExtractor extractor3 = makeExtractor("%{MYNUMBER:num}", config);

        final GrokExtractor extractor4 = makeExtractor("%{MYNUMBER:num}");

        assertThat(extractor1.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result("2015", "num", -1, -1));
        assertThat(extractor2.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result(2015, "num", -1, -1));
        assertThat(extractor3.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
        assertThat(extractor4.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
    }

    private GrokExtractor makeExtractor(String pattern) {
        return makeExtractor(pattern, new HashMap<>());
    }

    private GrokExtractor makeExtractor(String pattern, Map<String, Object> config) {
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