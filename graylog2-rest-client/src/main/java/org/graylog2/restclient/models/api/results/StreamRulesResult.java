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
package org.graylog2.restclient.models.api.results;

import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRulesResult {

    private final int total;
    private final List<StreamRuleSummaryResponse> streamRules;

    public StreamRulesResult(int total, List<StreamRuleSummaryResponse> streamRules) {
        this.total = total;
        this.streamRules = streamRules;
    }

    public int getTotal() {
        return total;
    }

    public List<StreamRuleSummaryResponse> getStreamRules() {
        return streamRules;
    }
}
