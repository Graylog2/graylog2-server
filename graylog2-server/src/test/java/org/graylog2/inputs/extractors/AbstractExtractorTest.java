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

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractExtractorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    protected MetricRegistry metricRegistry;

    @Before
    public void setUp() throws Exception {
        metricRegistry = new MetricRegistry();
    }

    static List<Converter> noConverters() {
        return Collections.emptyList();
    }

    static Map<String, Object> noConfig() {
        return Collections.emptyMap();
    }
}
