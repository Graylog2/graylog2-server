/*
 * Copyright 2013 TORCH UG
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
 */
package models;

import com.google.common.collect.Lists;
import models.api.responses.StreamRuleSummaryResponse;
import models.api.responses.StreamSummaryResponse;
import models.api.responses.TimestampResponse;

import java.util.List;

public class Stream {
	
	private final String id;
    private final String title;
    private final String creatorUserId;
    private final TimestampResponse createdAt;
    private final List<StreamRule> streamRules;

	public Stream(StreamSummaryResponse ssr) {
		this.id = ssr.id;
        this.title = ssr.title;
        this.creatorUserId = ssr.creatorUserId;
        this.createdAt = ssr.createdAt;

        this.streamRules = Lists.newArrayList();

        for (StreamRuleSummaryResponse streamRuleSummaryResponse : ssr.streamRules) {
            streamRules.add(new StreamRule(streamRuleSummaryResponse));
        }
	}

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public TimestampResponse getCreatedAt() {
        return createdAt;
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }
}
