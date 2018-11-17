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
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.search.responses.FieldStatsResult;
import org.graylog2.rest.models.search.responses.HistogramResult;
import org.graylog2.rest.models.search.responses.TermsResult;
import org.graylog2.rest.models.search.responses.TermsStatsResult;
import org.graylog2.rest.models.search.responses.TimeRange;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.utilities.SearchUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    protected static final String DEFAULT_SCROLL_BATCH_SIZE = "500";

    protected final Searches searches;
    private final ClusterConfigService clusterConfigService;
    private final DecoratorProcessor decoratorProcessor;

    public SearchResource(Searches searches,
                          ClusterConfigService clusterConfigService,
                          DecoratorProcessor decoratorProcessor) {
        this.searches = searches;
        this.clusterConfigService = clusterConfigService;
        this.decoratorProcessor = decoratorProcessor;
    }

    protected void validateInterval(String interval) {
        if (!SearchUtils.validateInterval(interval)) {
            LOG.warn("Invalid interval type <{}>. Returning HTTP 400.", interval);
            throw new BadRequestException("Invalid interval type: " + interval + "\"");
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
        final ArrayList<String> fieldList = Lists.newArrayList(Message.FIELD_TIMESTAMP);

        // skip the mandatory field timestamp
        for (String field : split) {
            if (Message.FIELD_TIMESTAMP.equals(field)) {
                continue;
            }
            fieldList.add(field);
        }

        return fieldList;
    }

    protected org.graylog2.indexer.results.FieldStatsResult fieldStats(String field, String query, String filter,
                                                                       org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) {
        try {
            return searches.fieldStats(field, query, filter, timeRange);
        } catch (FieldTypeException e) {
            try {
                LOG.debug("Stats query failed, make sure that field [{}] is a numeric type. Retrying without numeric statistics to calculate the field's cardinality.", field);
                return searches.fieldStats(field, query, filter, timeRange, true, false, true);
            } catch (FieldTypeException e1) {
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
                                                                          boolean includeCardinality) {
        try {
            return searches.fieldHistogram(
                    query,
                    field,
                    Searches.DateHistogramInterval.valueOf(interval),
                    filter,
                    timeRange,
                    true,
                    includeCardinality);
        } catch (FieldTypeException e) {
            try {
                LOG.debug("Field histogram query failed. Make sure that field [{}] is a numeric type. Retrying without numerical statistics.", field);
                return searches.fieldHistogram(
                        query,
                        field,
                        Searches.DateHistogramInterval.valueOf(interval),
                        filter,
                        timeRange,
                        false,
                        true);
            } catch (FieldTypeException e1) {
                final String msg = "Field histogram for field [" + field + "] failed while calculating its cardinality.";
                LOG.error(msg, ExceptionUtils.getRootCauseMessage(e1));
                throw new BadRequestException(msg, e1);
            }
        }
    }

    protected TermsResult buildTermsResult(org.graylog2.indexer.results.TermsResult tr) {
        return TermsResult.create(tr.tookMs(), tr.getTerms(), tr.termsMapping(), tr.getMissing(), tr.getOther(), tr.getTotal(), tr.getBuiltQuery());
    }

    protected TermsStatsResult buildTermsStatsResult(org.graylog2.indexer.results.TermsStatsResult tr) {
        return TermsStatsResult.create(tr.tookMs(), tr.getResults(), tr.getBuiltQuery());
    }

    protected SearchResponse buildSearchResponse(SearchResult sr,
                                                 org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange,
                                                 boolean decorate,
                                                 Optional<String> streamId) {
        final SearchResponse result = SearchResponse.create(sr.getOriginalQuery(),
            sr.getBuiltQuery(),
            indexRangeListToValueList(sr.getUsedIndices()),
            resultMessageListtoValueList(sr.getResults()),
            sr.getFields(),
            sr.tookMs(),
            sr.getTotalResults(),
            timeRange.getFrom(),
            timeRange.getTo());

        return decorate ? decoratorProcessor.decorate(result, streamId) : result;
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
        return resultMessages.stream()
            // TODO module merge: migrate to resultMessage.getMessage() instead of Map<String, Object> via getFields()
            .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
            .collect(Collectors.toList());
    }

    protected FieldStatsResult buildFieldStatsResult(org.graylog2.indexer.results.FieldStatsResult sr) {
        return FieldStatsResult.create(
            sr.tookMs(), sr.getCount(), sr.getSum(), sr.getSumOfSquares(), sr.getMean(),
            sr.getMin(), sr.getMax(), sr.getVariance(), sr.getStdDeviation(), sr.getBuiltQuery(), sr.getCardinality());

    }

    protected HistogramResult buildHistogramResult(org.graylog2.indexer.results.HistogramResult histogram) {
        final AbsoluteRange histogramBoundaries = histogram.getHistogramBoundaries();
        return HistogramResult.create(
            histogram.getInterval().toString().toLowerCase(Locale.ENGLISH),
            histogram.getResults(),
            histogram.tookMs(),
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
                    final String msg = "Not allowed to search with filter: [" + filter + "]. (Forbidden stream: " + streamId + ")";
                    LOG.warn(msg);
                    throw new ForbiddenException(msg);
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

    protected List<String> splitStackedFields(String stackedFieldsParam) {
        if (stackedFieldsParam == null || stackedFieldsParam.isEmpty()) {
            return Collections.emptyList();
        }
        return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(stackedFieldsParam);
    }
}
