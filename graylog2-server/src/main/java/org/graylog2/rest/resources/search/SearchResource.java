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

package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    protected void validateInterval(String interval) {
        try {
            Indexer.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid interval type. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected void checkQuery(String query) {
        if (query == null || query.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected void checkQueryAndField(String query, String field) {
        if (field == null || field.isEmpty() || query == null || query.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected void checkQueryAndInterval(String query, String interval) {
        if (query == null || query.isEmpty() || interval == null || interval.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected FieldStatsResult fieldStats(String field, String query, TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        try {
            return core.getIndexer().searches().fieldStats(field, query, timeRange);
        } catch(Searches.FieldTypeException e) {
            LOG.error("Stats query failed. Make sure that field [{}] is a numeric type.", field);
            throw new WebApplicationException(400);
        }
    }

    protected Map<String, Object> buildTermsResult(TermsResult tr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("time", tr.took().millis());
        result.put("terms", tr.getTerms());
        result.put("missing", tr.getMissing()); // The number of docs missing a value.
        result.put("other", tr.getOther()); // The count of terms other than the one provided by the entries.
        result.put("total", tr.getTotal()); // The total count of terms.

        return result;
    }

    protected Map<String, Object> buildSearchResult(SearchResult sr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("query", sr.getOriginalQuery());
        result.put("messages", sr.getResults());
        result.put("fields", sr.getFields());
        result.put("time", sr.took().millis());
        result.put("total_results", sr.getTotalResults());

        return result;
    }

    protected Map<String, Object> buildFieldStatsResult(FieldStatsResult sr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("time", sr.took().millis());
        result.put("count", sr.getCount());
        result.put("sum", sr.getSum());
        result.put("sum_of_squares", sr.getSumOfSquares());
        result.put("mean", sr.getMean());
        result.put("min", sr.getMin());
        result.put("max", sr.getMax());
        result.put("variance", sr.getVariance());
        result.put("std_deviation", sr.getStdDeviation());

        return result;
    }

    protected Map<String, Object> buildHistogramResult(DateHistogramResult histogram) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("interval", histogram.getInterval().toString().toLowerCase());
        result.put("results", histogram.getResults());
        result.put("time", histogram.took().millis());

        return result;
    }


}