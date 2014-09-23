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
package org.graylog2.rest.resources.search;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.Token;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.search.SearchParseException;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.*;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.search.responses.GenericError;
import org.graylog2.rest.resources.search.responses.QueryParseError;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);
    protected final Searches searches;

    @Inject
    public SearchResource(Searches searches) {
        this.searches = searches;
    }

    protected void validateInterval(String interval) {
        try {
            Searches.DateHistogramInterval.valueOf(interval);
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

    protected void checkQueryAndKeyword(String query, String keyword) {
        if (keyword == null || keyword.isEmpty() || query == null || query.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected void checkQueryAndField(String query, String field) {
        if (field == null || field.isEmpty() || query == null || query.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected void checkTermsStatsFields(String keyField, String valueField, String order) {
        if (keyField == null || keyField.isEmpty() || valueField == null || valueField.isEmpty() || order == null || order.isEmpty()) {
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

    protected void checkStringSet(String string) {
        if (string == null || string.isEmpty()) {
            LOG.warn("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
    }

    protected List<String> parseFields(String fields) {
        if (fields == null || fields.isEmpty()) {
            LOG.warn("Missing fields parameter. Returning HTTP 400");
            throw new BadRequestException("Missing required parameter `fields`");
        }
        return parseOptionalFields(fields);
    }

    protected List<String> parseOptionalFields(String fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        final Iterable<String> split = Splitter.on(',').omitEmptyStrings().trimResults().split(fields);
        final ArrayList<String> fieldList = Lists.newArrayList("timestamp");
        // skip the mandatory field timestamp
        for (String field : split) {
            if ("timestamp".equals(field)) {
                continue;
            }
            fieldList.add(field);
        }

        return fieldList;
    }

    protected FieldStatsResult fieldStats(String field, String query, TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        return fieldStats(field, query, null, timeRange);
    }

    protected FieldStatsResult fieldStats(String field, String query, String filter, TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        try {
            return searches.fieldStats(field, query, filter, timeRange);
        } catch(Searches.FieldTypeException e) {
            LOG.error("Stats query failed. Make sure that field [{}] is a numeric type.", field);
            throw new WebApplicationException(400);
        }
    }

    protected HistogramResult fieldHistogram(String field, String query, String interval, String filter, TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        try {
            return searches.fieldHistogram(
                    query,
                    field,
                    Searches.DateHistogramInterval.valueOf(interval),
                    filter,
                    timeRange
            );
        } catch(Searches.FieldTypeException e) {
            LOG.error("Field histogram query failed. Make sure that field [{}] is a numeric type.", field);
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
        result.put("built_query", tr.getBuiltQuery());

        return result;
    }

    protected Map<String, Object> buildTermsStatsResult(TermsStatsResult tr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("time", tr.took().millis());
        result.put("terms", tr.getResults());
        result.put("built_query", tr.getBuiltQuery());

        return result;
    }

    protected SearchResponse buildSearchResponse(SearchResult sr, TimeRange timeRange) {
        SearchResponse result = new SearchResponse();
        result.query = sr.getOriginalQuery();
        result.builtQuery = sr.getBuiltQuery();
        result.usedIndices = sr.getUsedIndices();
        result.messages = sr.getResults();
        result.fields = sr.getFields();
        result.time = sr.took().millis();
        result.totalResults = sr.getTotalResults();
        result.from = timeRange.getFrom();
        result.to = timeRange.getTo();

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
        result.put("built_query", sr.getBuiltQuery());

        return result;
    }

    protected Map<String, Object> buildHistogramResult(HistogramResult histogram) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("interval", histogram.getInterval().toString().toLowerCase());
        result.put("results", histogram.getResults());
        result.put("time", histogram.took().millis());
        result.put("built_query", histogram.getBuiltQuery());
        result.put("queried_timerange", histogram.getHistogramBoundaries().getLimits());

        return result;
    }

    protected Sorting buildSorting(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sorting.DEFAULT;
        }

        try {
            return Sorting.fromApiParam(sort);
        } catch(Exception e) {
            LOG.error("Falling back to default sorting.", e);
            return Sorting.DEFAULT;
        }
    }

    protected BadRequestException createRequestExceptionForParseFailure(String query, SearchPhaseExecutionException e) {
        LOG.warn("Unable to execute search: {}", e.getMessage());
        // we won't actually iterate over all of the shard failures, only the first one,
        // since we assume that parse errors happen on all of the shards.
        for (ShardSearchFailure failure : e.shardFailures()) {
            Throwable unwrapped = ExceptionsHelper.unwrapCause(failure.failure());
            if (!(unwrapped instanceof SearchParseException)) {
                LOG.warn("Unhandled ShardSearchFailure", e);
                return new BadRequestException();
            }
            Throwable rootCause = ((SearchParseException) unwrapped).getRootCause();
            if (rootCause instanceof ParseException) {

                Token currentToken = ((ParseException) rootCause).currentToken;
                SearchResponse sr = new SearchResponse();
                sr.query = query;
                sr.error = new QueryParseError();
                if (currentToken == null) {
                    LOG.warn("No position/token available for ParseException.");
                } else {
                    // scan for first usable token with position information
                    while (currentToken != null && sr.error.beginLine == 0) {
                        sr.error.beginColumn = currentToken.beginColumn;
                        sr.error.beginLine = currentToken.beginLine;
                        sr.error.endColumn = currentToken.endColumn;
                        sr.error.endLine = currentToken.endLine;

                        currentToken = currentToken.next;
                    }
                }
                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(json(sr)).build());
            } else if(rootCause instanceof NumberFormatException) {
                final SearchResponse sr = new SearchResponse();
                sr.query = query;
                sr.genericError = new GenericError();
                sr.genericError.exceptionName = rootCause.getClass().getCanonicalName();
                sr.genericError.message = rootCause.getMessage();
                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(json(sr)).build());
            } else {
                LOG.info("Root cause of SearchParseException has unexpected, generic type!" + rootCause.getClass());
                final SearchResponse sr = new SearchResponse();
                sr.query = query;
                sr.genericError = new GenericError();
                sr.genericError.exceptionName = rootCause.getClass().getCanonicalName();
                sr.genericError.message = rootCause.getMessage();
                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(json(sr)).build());
            }
        }

        return new BadRequestException();
    }

    public void checkSearchPermission(String filter, String searchPermission) {
        if (filter == null || filter.equals("*") || filter.isEmpty()) {
            checkPermission(searchPermission);
        } else {
            if(!filter.startsWith("streams:")) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            String[] parts = filter.split(":");
            if (parts.length <= 1) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }
            
            String streamList = parts[1];
            String[] streams = streamList.split(",");
            if (streams.length == 0 ) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            for(String streamId : streams) {
                if (!isPermitted(RestPermissions.STREAMS_READ, streamId)) {
                    LOG.warn("Not allowed to search with filter: [" + filter + "]. (Forbidden stream: " + streamId + ")");
                    throw new ForbiddenException();
                }
            }
        }
    }

    protected Runnable createScrollChunkProducer(final ScrollResult scroll,
                                                 final ChunkedOutput<ScrollResult.ScrollChunk> output,
                                                 final int limit) {
        return new Runnable() {
            private int collectedHits = 0;

            @Override
            public void run() {
                try {
                    ScrollResult.ScrollChunk chunk = scroll.nextChunk();
                    while (chunk != null) {
                        LOG.debug("[{}] Writing scroll chunk with {} messages",
                                  scroll.getQueryHash(),
                                  chunk.getMessages().size());
                        if (output.isClosed()) {
                            LOG.debug("[{}] Client connection is closed, client disconnected. Aborting scroll.",
                                      scroll.getQueryHash());
                            scroll.cancel();
                            return;
                        }
                        output.write(chunk);
                        collectedHits += chunk.getMessages().size();
                        if (limit != 0 && collectedHits >= limit) {
                            scroll.cancel();
                            output.close();
                            return;
                        }
                        chunk = scroll.nextChunk();
                    }
                    LOG.debug("[{}] Reached end of scroll result.", scroll.getQueryHash());
                    output.close();
                } catch (IOException e) {
                    LOG.warn("[{}] Could not close chunked output stream for query scroll.", scroll.getQueryHash());
                }
            }
        };
    }
}