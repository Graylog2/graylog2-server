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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.Token;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.search.SearchParseException;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.search.responses.FieldStatsResult;
import org.graylog2.rest.resources.search.responses.GenericError;
import org.graylog2.rest.resources.search.responses.HistogramResult;
import org.graylog2.rest.resources.search.responses.QueryParseError;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.rest.resources.search.responses.TermsResult;
import org.graylog2.rest.resources.search.responses.TermsStatsResult;
import org.graylog2.rest.resources.search.responses.TimeRange;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class SearchResource extends RestResource {
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
            throw new BadRequestException("Invalid interval type", e);
        }
    }

    protected List<String> parseFields(String fields) {
        if (isNullOrEmpty(fields)) {
            LOG.warn("Missing fields parameter. Returning HTTP 400");
            throw new BadRequestException("Missing required parameter `fields`");
        }
        return parseOptionalFields(fields);
    }

    protected List<String> parseOptionalFields(String fields) {
        if (isNullOrEmpty(fields)) {
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

    protected org.graylog2.indexer.results.FieldStatsResult fieldStats(String field, String query, String filter,
                                                                       org.graylog2.indexer.searches.timeranges.TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        try {
            return searches.fieldStats(field, query, filter, timeRange);
        } catch (Searches.FieldTypeException e) {
            LOG.error("Stats query failed. Make sure that field [{}] is a numeric type.", field);
            throw new BadRequestException();
        }
    }

    protected org.graylog2.indexer.results.HistogramResult fieldHistogram(String field, String query, String interval,
                                                                          String filter, org.graylog2.indexer.searches.timeranges.TimeRange timeRange) throws IndexHelper.InvalidRangeFormatException {
        try {
            return searches.fieldHistogram(
                    query,
                    field,
                    Searches.DateHistogramInterval.valueOf(interval),
                    filter,
                    timeRange
            );
        } catch (Searches.FieldTypeException e) {
            LOG.error("Field histogram query failed. Make sure that field [{}] is a numeric type.", field);
            throw new BadRequestException();
        }
    }

    protected TermsResult buildTermsResult(org.graylog2.indexer.results.TermsResult tr) {
        return TermsResult.create(tr.took().millis(), tr.getTerms(), tr.getMissing(), tr.getOther(), tr.getTotal(), tr.getBuiltQuery());
    }

    protected TermsStatsResult buildTermsStatsResult(org.graylog2.indexer.results.TermsStatsResult tr) {
        return TermsStatsResult.create(tr.took().millis(), tr.getResults(), tr.getBuiltQuery());
    }

    protected SearchResponse buildSearchResponse(SearchResult sr, org.graylog2.indexer.searches.timeranges.TimeRange timeRange) {
        return SearchResponse.create(sr.getOriginalQuery(),
                sr.getBuiltQuery(),
                sr.getUsedIndices(),
                sr.getResults(),
                sr.getFields(),
                sr.took().millis(),
                sr.getTotalResults(),
                timeRange.getFrom(),
                timeRange.getTo());
    }

    protected FieldStatsResult buildFieldStatsResult(org.graylog2.indexer.results.FieldStatsResult sr) {
        return FieldStatsResult.create(
                sr.took().millis(), sr.getCount(), sr.getSum(), sr.getSumOfSquares(), sr.getMean(),
                sr.getMin(), sr.getMax(), sr.getVariance(), sr.getStdDeviation(), sr.getBuiltQuery());

    }

    protected HistogramResult buildHistogramResult(org.graylog2.indexer.results.HistogramResult histogram) {
        final AbsoluteRange histogramBoundaries = histogram.getHistogramBoundaries();
        return HistogramResult.create(
                histogram.getInterval().toString().toLowerCase(),
                histogram.getResults(),
                histogram.took().millis(),
                histogram.getBuiltQuery(),
                TimeRange.create(histogramBoundaries.getFrom(), histogramBoundaries.getTo()));
    }

    protected Sorting buildSorting(String sort) {
        if (isNullOrEmpty(sort)) {
            return Sorting.DEFAULT;
        }

        try {
            return Sorting.fromApiParam(sort);
        } catch (Exception e) {
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
                final QueryParseError queryParseError;
                if (currentToken == null) {
                    LOG.warn("No position/token available for ParseException.");
                    queryParseError = QueryParseError.create(query);
                } else {
                    // scan for first usable token with position information
                    int beginColumn = 0;
                    int beginLine = 0;
                    int endColumn = 0;
                    int endLine = 0;
                    while (currentToken != null && beginLine == 0) {
                        beginColumn = currentToken.beginColumn;
                        beginLine = currentToken.beginLine;
                        endColumn = currentToken.endColumn;
                        endLine = currentToken.endLine;

                        currentToken = currentToken.next;
                    }

                    queryParseError = QueryParseError.create(query, beginColumn, beginLine, endColumn, endLine);
                }

                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(queryParseError).build());
            } else if (rootCause instanceof NumberFormatException) {
                final GenericError genericError = GenericError.create(query, rootCause.getClass().getCanonicalName(), rootCause.getMessage());
                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(genericError).build());
            } else {
                LOG.info("Root cause of SearchParseException has unexpected, generic type!" + rootCause.getClass());
                final GenericError genericError = GenericError.create(query, rootCause.getClass().getCanonicalName(), rootCause.getMessage());
                return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(genericError).build());
            }
        }

        return new BadRequestException();
    }

    public void checkSearchPermission(String filter, String searchPermission) {
        if (isNullOrEmpty(filter) || "*".equals(filter)) {
            checkPermission(searchPermission);
        } else {
            if (!filter.startsWith("streams:")) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            String[] parts = filter.split(":");
            if (parts.length <= 1) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            String streamList = parts[1];
            String[] streams = streamList.split(",");
            if (streams.length == 0) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            for (String streamId : streams) {
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