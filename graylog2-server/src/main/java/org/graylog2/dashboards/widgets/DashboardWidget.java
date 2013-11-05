/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.dashboards.widgets;

import org.graylog2.indexer.searches.timeranges.*;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.rest.resources.dashboards.requests.AddWidgetRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class DashboardWidget implements EmbeddedPersistable {

    public enum Type {
        SEARCH_RESULT_COUNT
    }

    private final Type type;
    private final String id;
    private final Map<String, Object> config;
    private final String creatorUserId;

    protected DashboardWidget(Type type, String id, Map<String, Object> config, String creatorUserId) {
        this.type =  type;
        this.id = id;
        this.config = config;
        this.creatorUserId = creatorUserId;
    }

    public static DashboardWidget fromRequest(AddWidgetRequest awr) throws NoSuchWidgetTypeException, InvalidRangeParametersException {
        Type type;
        try {
            type = Type.valueOf(awr.type.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException();
        }

        String id = UUID.randomUUID().toString();

        // Build timerange.
        TimeRange timeRange;

        if (awr.config.get("range_type") == null) {
            throw new InvalidRangeParametersException("range_type not set");
        }

        if (awr.config.get("range_type").equals("relative")) {
            timeRange = new RelativeRange(Integer.parseInt((String) awr.config.get("range")));
        } else if(awr.config.get("range_type").equals("keyword")) {
            timeRange = new KeywordRange((String) awr.config.get("keyword"));
        } else if(awr.config.get("range_type").equals("absolute")) {
            timeRange = new AbsoluteRange((String) awr.config.get("from"), (String) awr.config.get("to"));
        } else {
            throw new InvalidRangeParametersException("range_type not recognized");
        }

        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(id, awr.config, (String) awr.config.get("query"), timeRange, awr.creatorUserId);
            default:
                throw new NoSuchWidgetTypeException();
        }
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public Map<String, Object> getPersistedFields() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("type", type.toString().toLowerCase());
            put("creator_user_id", creatorUserId);
            put("config", getPersistedConfig());
        }};
    }

    public abstract Map<String, Object> getPersistedConfig();

    public static class NoSuchWidgetTypeException extends Exception {
    }

}
