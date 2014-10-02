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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

@JsonAutoDetect
public class SearchResponse {
    public String query;
    public String builtQuery;
    public Set<IndexRange> usedIndices;
    public List<ResultMessage> messages;
    public Set<String> fields;
    public long time;
    public long totalResults;
    public DateTime from;
    public DateTime to;

    public QueryParseError error;
    public GenericError genericError;
}
