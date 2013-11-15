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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import models.api.responses.streams.StreamRuleSummaryResponse;
import models.api.responses.streams.StreamSummaryResponse;
import models.api.responses.TimestampResponse;
import org.joda.time.DateTime;

import java.util.List;

public class Stream {

    public interface Factory {
        public Stream fromSummaryResponse(StreamSummaryResponse ssr);
    }
	
	private final String id;
    private final String title;
    private final String creatorUserId;
    private final String createdAt;
    private final List<StreamRule> streamRules;
    private final Boolean disabled;

    private UserService userService;

	@AssistedInject
    private Stream(UserService userService, @Assisted StreamSummaryResponse ssr) {
		this.id = ssr.id;
        this.title = ssr.title;
        this.creatorUserId = ssr.creatorUserId;
        this.createdAt = ssr.createdAt;

        this.streamRules = Lists.newArrayList();

        this.disabled = ssr.disabled;

        this.userService = userService;

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

    public User getCreatorUser() {
        return userService.load(this.creatorUserId);
    }

    public DateTime getCreatedAt() {
        return DateTime.parse(createdAt);
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public Boolean getDisabled() {
        return (disabled != null && disabled);
    }
}
