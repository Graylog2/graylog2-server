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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.BasicDBObject;
import org.graylog2.Core;
import org.graylog2.indexer.searches.timeranges.*;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.rest.resources.dashboards.requests.AddWidgetRequest;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class DashboardWidget implements EmbeddedPersistable {

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT
    }

    private static final String RESULT_CACHE_KEY = "result";

    public static final int DEFAULT_CACHE_TIME = 10;

    private final Core core;
    private final Type type;
    private final String id;
    private final Map<String, Object> config;
    private final String creatorUserId;
    private int cacheTime;
    private String description;

    private Cache<String, ComputationResult> cache;

    protected DashboardWidget(Core core, Type type, String id, String description, int cacheTimeS, Map<String, Object> config, String creatorUserId) {
        this.core = core;
        this.type =  type;
        this.id = id;
        this.config = config;
        this.creatorUserId = creatorUserId;
        this.description = description;

        if (cacheTimeS < 1) {
            this.cacheTime = DEFAULT_CACHE_TIME;
        } else {
            this.cacheTime = cacheTimeS;
        }

        this.cache = buildCache(this.cacheTime);
    }

    private Cache<String, ComputationResult> buildCache(int cacheTime) {
        return CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(cacheTime, TimeUnit.SECONDS)
                .build();
    }

    public static DashboardWidget fromRequest(Core core, AddWidgetRequest awr) throws NoSuchWidgetTypeException, InvalidRangeParametersException {
        Type type;
        try {
            type = Type.valueOf(awr.type.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException("No such widget type <" + awr.type + ">");
        }

        String id = UUID.randomUUID().toString();

        // Build timerange.
        TimeRange timeRange;

        if (!awr.config.containsKey("range_type")) {
            throw new InvalidRangeParametersException("range_type not set");
        }

        String rangeType = (String) awr.config.get("range_type");

        if (rangeType.equals("relative")) {
            timeRange = new RelativeRange(Integer.parseInt((String) awr.config.get("range")));
        } else if(rangeType.equals("keyword")) {
            timeRange = new KeywordRange((String) awr.config.get("keyword"));
        } else if(rangeType.equals("absolute")) {
            timeRange = new AbsoluteRange((String) awr.config.get("from"), (String) awr.config.get("to"));
        } else {
            throw new InvalidRangeParametersException("range_type not recognized");
        }

        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(core, id, awr.description, 0, awr.config, (String) awr.config.get("query"), timeRange, awr.creatorUserId);
            case STREAM_SEARCH_RESULT_COUNT:
                return new StreamSearchResultCountWidget(core, id, awr.description, 0, awr.config, (String) awr.config.get("query"), timeRange, awr.creatorUserId);
            default:
                throw new NoSuchWidgetTypeException();
        }
    }

    public static DashboardWidget fromPersisted(Core core, BasicDBObject fields) throws NoSuchWidgetTypeException, InvalidRangeParametersException {
        Type type;
        try {
            type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException();
        }

        BasicDBObject config = (BasicDBObject) fields.get("config");

        // Build timerange.
        BasicDBObject timerangeConfig = (BasicDBObject) config.get("timerange");
        TimeRange timeRange;

        if (!timerangeConfig.containsField("type")) {
            throw new InvalidRangeParametersException("range type not set");
        }

        String rangeType = (String) timerangeConfig.get("type");

        if (rangeType.equals("relative")) {
            timeRange = new RelativeRange((Integer) timerangeConfig.get("range"));
        } else if(rangeType.equals("keyword")) {
            timeRange = new KeywordRange((String) timerangeConfig.get("keyword"));
        } else if(rangeType.equals("absolute")) {

            String from = new DateTime(timerangeConfig.get("from")).toString(Tools.ES_DATE_FORMAT);
            String to = new DateTime(timerangeConfig.get("to")).toString(Tools.ES_DATE_FORMAT);

            timeRange = new AbsoluteRange(from, to);
        } else {
            throw new InvalidRangeParametersException("range_type not recognized");
        }

        // Is a description set?
        String description = null;
        if (fields.containsField("description")) {
            description = (String) fields.get("description");
        }

        // Do we have a configured cache time?
        int cacheTime = 0;
        if (fields.containsField("cache_time")) {
            cacheTime = (Integer) fields.get("cache_time");
        }

        // XXX TODO: these long constructors suck and 90% of it can be unified in a step before
        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(core, (String) fields.get("id"), description, cacheTime, config, (String) config.get("query"), timeRange, (String) fields.get("creator_user_id"));
            case STREAM_SEARCH_RESULT_COUNT:
                return new StreamSearchResultCountWidget(core, (String) fields.get("id"), description, cacheTime, config, (String) config.get("query"), timeRange, (String) fields.get("creator_user_id"));
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCacheTime(int cacheTime) {
        this.cache = buildCache(cacheTime);
        this.cacheTime = cacheTime;
    }

    public int getCacheTime() {
        return cacheTime;
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
            put("description", description);
            put("cache_time", cacheTime);
            put("creator_user_id", creatorUserId);
            put("config", getPersistedConfig());
        }};
    }

    public ComputationResult getComputationResult() throws ExecutionException {
        return cache.get(RESULT_CACHE_KEY, new Callable<ComputationResult>() {
            @Override
            public ComputationResult call() throws Exception {
                ComputationResult result;

                Timer.Context timer = getCalculationTimer().time();
                try {
                    result = compute();
                } finally {
                    timer.stop();
                }

                getCalculationMeter().mark();

                return result;
            }
        });
    }

    public abstract Map<String, Object> getPersistedConfig();
    protected abstract ComputationResult compute();


    private Timer getCalculationTimer() {
        return core.metrics().timer(MetricRegistry.name(this.getClass(), getId(), "calculationTime"));
    }

    private Meter getCalculationMeter() {
        return core.metrics().meter(MetricRegistry.name(this.getClass(), getId(), "calculations"));
    }

    public static class NoSuchWidgetTypeException extends Exception {

        public NoSuchWidgetTypeException() {
            super();
        }

        public NoSuchWidgetTypeException(String msg) {
            super(msg);
        }

    }

}
