/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.rest.resources;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RestResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    protected Core core;

    @QueryParam("pretty")
    boolean prettyPrint;

	protected RestResource() { /* */ }

    protected int page(int page) {
        if (page <= 0) {
            return 0;
        }

        return page-1;
    }

	protected ObjectId loadObjectId(String id) {
		try {
			return new ObjectId(id);
		} catch (IllegalArgumentException e) {
        	LOG.error("Invalid ObjectID \"" + id + "\". Returning HTTP 400.");
        	throw new WebApplicationException(400);
		}
	}

    protected String json(Object x) {
        try {
            if (this.prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(x);
            } else {
                return objectMapper.writeValueAsString(x);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error while generating JSON", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected Map<String, Long> bytesToValueMap(long bytes) {
        Map<String, Long> r = Maps.newHashMap();

        int kb = 1024;
        int mb = kb*1024;

        r.put("bytes", bytes);
        r.put("kilobytes", bytes/kb);
        r.put("megabytes", bytes/mb);

        return r;
    }

    protected Map<String, Object> buildMetricsMap(Timer t) {
        Map<String, Object> metrics = Maps.newHashMap();

        if (t == null) {
            return metrics;
        }

        TimeUnit timeUnit = TimeUnit.MICROSECONDS;

        Map<String, Object> time = Maps.newHashMap();
        time.put("max", TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMax(), TimeUnit.NANOSECONDS));
        time.put("min", TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMin(), TimeUnit.NANOSECONDS));
        time.put("mean", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getMean(), TimeUnit.NANOSECONDS));
        time.put("95th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get95thPercentile(), TimeUnit.NANOSECONDS));
        time.put("98th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get98thPercentile(), TimeUnit.NANOSECONDS));
        time.put("99th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get99thPercentile(), TimeUnit.NANOSECONDS));
        time.put("std_dev", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getStdDev(), TimeUnit.NANOSECONDS));

        Map<String, Object> rate = Maps.newHashMap();
        rate.put("one_minute", t.getOneMinuteRate());
        rate.put("five_minute", t.getFiveMinuteRate());
        rate.put("fifteen_minute", t.getFifteenMinuteRate());
        rate.put("total", t.getCount());
        rate.put("mean", t.getMeanRate());

        metrics.put("rate_unit", "events/second");
        metrics.put("duration_unit", timeUnit.toString().toLowerCase());
        metrics.put("time", time);
        metrics.put("rate", rate);

        return metrics;
    }

}
