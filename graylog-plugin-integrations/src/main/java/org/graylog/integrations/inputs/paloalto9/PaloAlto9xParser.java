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
package org.graylog.integrations.inputs.paloalto9;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageType;
import org.graylog.integrations.inputs.paloalto.PaloAltoTypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PaloAlto9xParser {
    private static final Logger LOG = LoggerFactory.getLogger(PaloAlto9xParser.class);

    private final Map<PaloAltoMessageType, PaloAltoTypeParser> parsers;

    public PaloAlto9xParser() {
        this(new PaloAltoTypeParser(PaloAlto9xTemplates.configTemplate(), PaloAltoMessageType.CONFIG),
                new PaloAltoTypeParser(PaloAlto9xTemplates.correlationTemplate(), PaloAltoMessageType.CORRELATION),
                new PaloAltoTypeParser(PaloAlto9xTemplates.globalProtectTemplate(), PaloAltoMessageType.GLOBAL_PROTECT),
                new PaloAltoTypeParser(PaloAlto9xTemplates.hipTemplate(), PaloAltoMessageType.HIP),
                new PaloAltoTypeParser(PaloAlto9xTemplates.systemTemplate(), PaloAltoMessageType.SYSTEM),
                new PaloAltoTypeParser(PaloAlto9xTemplates.threatTemplate(), PaloAltoMessageType.THREAT),
                new PaloAltoTypeParser(PaloAlto9xTemplates.trafficTemplate(), PaloAltoMessageType.TRAFFIC));
    }

    /* Package Private */ PaloAlto9xParser(PaloAltoTypeParser configParser,
                                           PaloAltoTypeParser correlationParser,
                                           PaloAltoTypeParser globalProtectParser,
                                           PaloAltoTypeParser hipParser,
                                           PaloAltoTypeParser systemParser,
                                           PaloAltoTypeParser threatParser,
                                           PaloAltoTypeParser trafficParser) {
        parsers = Maps.newHashMap();
        parsers.put(PaloAltoMessageType.CONFIG, configParser);
        parsers.put(PaloAltoMessageType.CORRELATION, correlationParser);
        parsers.put(PaloAltoMessageType.GLOBAL_PROTECT, globalProtectParser);
        parsers.put(PaloAltoMessageType.HIP, hipParser);
        parsers.put(PaloAltoMessageType.SYSTEM, systemParser);
        parsers.put(PaloAltoMessageType.THREAT, threatParser);
        parsers.put(PaloAltoMessageType.TRAFFIC, trafficParser);
    }

    public ImmutableMap<String, Object> parseFields(PaloAltoMessageType type, List<String> fields) {
        if (parsers.containsKey(type)) {
            PaloAltoTypeParser parser = parsers.get(type);
            return parser.parseFields(fields);
        }
        LOG.error("Unsupported PAN type [{}]. Not adding any parsed fields.", type);
        return ImmutableMap.of();
    }
}
