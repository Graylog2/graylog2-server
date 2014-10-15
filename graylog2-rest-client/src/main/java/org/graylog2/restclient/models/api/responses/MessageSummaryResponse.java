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
package org.graylog2.restclient.models.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;

public class MessageSummaryResponse {

	public Map<String, Object> message;
	public String index;
	public String nodeId;
    @JsonProperty("highlight_ranges")
    public Map<String, List<HighlightRange>> highlightRanges;

    public DateTime getTimestamp() {
        return new DateTime(message.get("timestamp"), DateTimeZone.UTC);
    }

}
