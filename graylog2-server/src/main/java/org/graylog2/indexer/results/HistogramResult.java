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
package org.graylog2.indexer.results;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import java.util.Map;

public abstract class HistogramResult extends IndexQueryResult {

    private AbsoluteRange boundaries;

    public HistogramResult(String originalQuery, String builtQuery, long tookMs) {
        super(originalQuery, builtQuery, tookMs);
    }

    public abstract Searches.DateHistogramInterval getInterval();
    public abstract Map getResults();

    /*
     * Extract from and to fields from the built query to determine
     * histogram boundaries.
     */
    public AbsoluteRange getHistogramBoundaries() {
        if (boundaries == null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonParser jp = mapper.getFactory().createParser(getBuiltQuery());
                JsonNode rootNode = mapper.readTree(jp);
                JsonNode timestampNode = rootNode.findValue("range").findValue("timestamp");
                String from = Tools.elasticSearchTimeFormatToISO8601(timestampNode.findValue("from").asText());
                String to = Tools.elasticSearchTimeFormatToISO8601(timestampNode.findValue("to").asText());
                boundaries = AbsoluteRange.create(from, to);
            } catch (Exception ignored) {}
        }

        return boundaries;
    }
}
