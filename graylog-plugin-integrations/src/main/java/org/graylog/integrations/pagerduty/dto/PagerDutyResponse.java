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
package org.graylog.integrations.pagerduty.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @author Edgar Molina
 *
 */
public class PagerDutyResponse {
    @JsonProperty("status")
    private String status;
    @JsonProperty("message")
    private String message;
    @JsonProperty("dedup_key")
    private String dedupKey;
    @JsonProperty("errors")
    private List<String> errors;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public List<String> getErrors() {
        return errors;
    }
}
