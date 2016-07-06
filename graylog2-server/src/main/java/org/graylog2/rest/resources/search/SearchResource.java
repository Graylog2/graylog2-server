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
package org.graylog2.rest.resources.search;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.Token;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.search.responses.FieldStatsResult;
import org.graylog2.rest.models.search.responses.HistogramResult;
import org.graylog2.rest.models.search.responses.TermsResult;
import org.graylog2.rest.models.search.responses.TermsStatsResult;
import org.graylog2.rest.models.search.responses.TimeRange;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.QueryParseError;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    protected final Searches searches;
    private final ClusterConfigService clusterConfigService;
    private final DecoratorProcessor decoratorProcessor;

    @Inject
    public SearchResource(Searches searches,
                          ClusterConfigService clusterConfigService,
                          DecoratorProcessor decoratorProcessor) {
        this.searches = searches;
        this.clusterConfigService = clusterConfigService;
        this.decoratorProcessor = decoratorProcessor;
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
                                                                       org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) throws InvalidRangeFormatException {
        try {
            return searches.fieldStats(field, query, filter, timeRange);
        } catch (Searches.FieldTypeException e) {
            try {
                LOG.debug("Stats query failed, make sure that field [{}] is a numeric type. Retrying without numeric statistics to calculate the field's cardinality.", field);
                return searches.fieldStats(field, query, filter, timeRange, true, false, true);
            } catch (Searches.FieldTypeException e1) {
                LOG.error("Retrieving field statistics for field {} failed while calculating the cardinality. Cause: {}", field, ExceptionUtils.getRootCauseMessage(e1));
                throw new BadRequestException("Field " + field + " is not of a numeric type and the cardinality could not be calculated either.", e1);
            }
        }
    }

    protected org.graylog2.indexer.results.HistogramResult fieldHistogram(String field,
                                                                          String query,
                                                                          String interval,
                                                                          String filter,
                                                                          org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange,
                                                                          boolean includeCardinality) throws InvalidRangeFormatException {
        try {
            return searches.fieldHistogram(
                query,
                field,
                Searches.DateHistogramInterval.valueOf(interval),
                filter,
                timeRange,
                includeCardinality);
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

    protected SearchResponse buildSearchResponse(SearchResult sr, org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) {
        SearchResponse result = SearchResponse.create(sr.getOriginalQuery(),
            sr.getBuiltQuery(),
            indexRangeListToValueList(sr.getUsedIndices()),
            resultMessageListtoValueList(sr.getResults()),
            sr.getFields(),
            sr.took().millis(),
            sr.getTotalResults(),
            timeRange.getFrom(),
            timeRange.getTo());

        return decoratorProcessor.decorate(result);
    }

    protected Set<IndexRangeSummary> indexRangeListToValueList(Set<IndexRange> indexRanges) {
        final Set<IndexRangeSummary> result = Sets.newHashSetWithExpectedSize(indexRanges.size());

        for (IndexRange indexRange : indexRanges) {
            result.add(IndexRangeSummary.create(
                indexRange.indexName(),
                indexRange.begin(),
                indexRange.end(),
                indexRange.calculatedAt(),
                indexRange.calculationDuration()));
        }

        return result;
    }

    protected List<ResultMessageSummary> resultMessageListtoValueList(List<ResultMessage> resultMessages) {
        final Collection<ResultMessage> transformedMessages = decoratorProcessor.decorate(resultMessages);
        return transformedMessages.stream()
            // TODO module merge: migrate to resultMessage.getMessage() instead of Map<String, Object> via getFields()
            .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
            .collect(Collectors.toList());
    }

    protected FieldStatsResult buildFieldStatsResult(org.graylog2.indexer.results.FieldStatsResult sr) {
        return FieldStatsResult.create(
            sr.took().millis(), sr.getCount(), sr.getSum(), sr.getSumOfSquares(), sr.getMean(),
            sr.getMin(), sr.getMax(), sr.getVariance(), sr.getStdDeviation(), sr.getBuiltQuery(), sr.getCardinality());

    }

    protected HistogramResult buildHistogramResult(org.graylog2.indexer.results.HistogramResult histogram) {
        final AbsoluteRange histogramBoundaries = histogram.getHistogramBoundaries();
        return HistogramResult.create(
            histogram.getInterval().toString().toLowerCase(Locale.ENGLISH),
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

    protected ChunkedOutput<ScrollResult.ScrollChunk> buildChunkedOutput(final ScrollResult scroll, int limit) {
        final ChunkedOutput<ScrollResult.ScrollChunk> output = new ChunkedOutput<>(ScrollResult.ScrollChunk.class);

        LOG.debug("[{}] Scroll result contains a total of {} messages", scroll.getQueryHash(), scroll.totalHits());
        Runnable scrollIterationAction = createScrollChunkProducer(scroll, output, limit);
        // TODO use a shared executor for async responses here instead of a single thread that's not limited
        new Thread(scrollIterationAction).start();
        return output;
    }

    protected BadRequestException createRequestExceptionForParseFailure(String query, SearchPhaseExecutionException e) {
        LOG.warn("Unable to execute search: {}", e.getMessage());

        QueryParseError errorMessage = QueryParseError.create(query, "Unable to execute search", e.getClass().getCanonicalName());

        // We're so going to hell for this…
        if (e.toString().contains("nested: QueryParsingException")) {
            final QueryParser queryParser = new QueryParser("", new StandardAnalyzer());
            try {
                queryParser.parse(query);
            } catch (ParseException parseException) {
                // FIXME I have no idea why this is necessary but without that call currentToken will be null.
                final ParseException exception = queryParser.generateParseException();
                Token currentToken = exception.currentToken;
                if (currentToken == null) {
                    LOG.warn("No position/token available for ParseException.", parseException);
                    errorMessage = QueryParseError.create(
                        query,
                        parseException.getMessage(),
                        parseException.getClass().getCanonicalName());
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

                    errorMessage = QueryParseError.create(
                        query,
                        beginColumn,
                        beginLine,
                        endColumn,
                        endLine,
                        parseException.getMessage(),
                        parseException.getClass().getCanonicalName());
                }
            }
        }

        return new BadRequestException(Response
            .status(Response.Status.BAD_REQUEST)
            .entity(errorMessage)
            .build());
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

    protected org.graylog2.plugin.indexer.searches.timeranges.TimeRange restrictTimeRange(final org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) {
        final DateTime originalFrom = timeRange.getFrom();
        final DateTime to = timeRange.getTo();
        final DateTime from;

        final SearchesClusterConfig config = clusterConfigService.get(SearchesClusterConfig.class);

        if (config == null || Period.ZERO.equals(config.queryTimeRangeLimit())) {
            from = originalFrom;
        } else {
            final DateTime limitedFrom = to.minus(config.queryTimeRangeLimit());
            from = limitedFrom.isAfter(originalFrom) ? limitedFrom : originalFrom;
        }

        return AbsoluteRange.create(from, to);
    }
}
