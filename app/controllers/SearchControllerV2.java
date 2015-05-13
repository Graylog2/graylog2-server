/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;
import lib.SearchTools;
import lib.security.RestPermissions;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Field;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.SavedSearch;
import org.graylog2.restclient.models.SavedSearchService;
import org.graylog2.restclient.models.SearchSort;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.UniversalSearch;
import org.graylog2.restclient.models.api.responses.QueryParseError;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import org.graylog2.restclient.models.api.results.MessageResult;
import org.graylog2.restclient.models.api.results.SearchResult;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static views.helpers.Permissions.isPermitted;

// TODO none of this makes sense, it's just a start.
public class SearchControllerV2 extends AuthenticatedController {
    // guess high, so we never have a bad resolution
    private static final int DEFAULT_ASSUMED_GRAPH_RESOLUTION = 4000;

    @Inject
    protected UniversalSearch.Factory searchFactory;
    @Inject
    protected MessagesService messagesService;
    @Inject
    protected SavedSearchService savedSearchService;
    @Inject
    private ServerNodes serverNodes;
    @Inject
    private StreamService streamService;
    @Inject
    private NodeService nodeService;
    @Inject
    private ObjectMapper objectMapper;

    public Result index(String q,
                        String rangeType,
                        int relative,
                        String from, String to,
                        String keyword,
                        String interval,
                        int page,
                        String savedSearchId,
                        String sortField, String sortOrder,
                        String fields,
                        int displayWidth) {
        if (isPermitted(RestPermissions.SEARCHES_ABSOLUTE)
                || isPermitted(RestPermissions.SEARCHES_RELATIVE)
                || isPermitted(RestPermissions.SEARCHES_KEYWORD)) {
            SearchSort sort = buildSearchSort(sortField, sortOrder);

            return renderSearch(q,
                                Strings.isNullOrEmpty(rangeType) ? "relative" : rangeType, // stupid
                                relative,
                                from,
                                to,
                                keyword,
                                interval,
                                page,
                                savedSearchId,
                                fields,
                                displayWidth,
                                sort,
                                null,
                                null);
        } else {
            return redirect(routes.StreamsController.index());
        }
    }

    protected Result renderSearch(String q,
                                  String rangeType,
                                  int relative,
                                  String from,
                                  String to,
                                  String keyword,
                                  String interval,
                                  int page,
                                  String savedSearchId,
                                  String fields,
                                  int displayWidth,
                                  SearchSort sort,
                                  Stream stream,
                                  String filter) {
        UniversalSearch search;
        try {
            search = getSearch(q, filter, rangeType, relative, from, to, keyword, page, sort);
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }


        SearchResult searchResult;
        DateHistogramResult histogramResult;
        SavedSearch savedSearch = null;
        Set<String> selectedFields = getSelectedFields(fields);
        String formattedHistogramResults;
        Set<StreamDescription> streams;
        Set<InputDescription> inputs = Sets.newHashSet();
        Map<String, NodeDescription> nodes = Maps.newHashMap();

        nodes.putAll(Maps.transformEntries(serverNodes.asMap(),
                                           new Maps.EntryTransformer<String, Node, NodeDescription>() {
                                               @Override
                                               public NodeDescription transformEntry(
                                                       @Nullable String key,
                                                       @Nullable Node value) {
                                                   return new NodeDescription(value);
                                               }
                                           }));
        try {
            if (savedSearchId != null && !savedSearchId.isEmpty()) {
                savedSearch = savedSearchService.get(savedSearchId);
            }


            searchResult = search.search();
            // TODO create a bulk call to get stream and input details (and restrict the fields that come back)
            final Set<String> streamIds = Sets.newHashSet();
            final HashMultimap<String, String> usedInputIds = HashMultimap.create();
            final HashMultimap<String, String> usedRadioIds = HashMultimap.create();

            for (MessageResult messageResult : searchResult.getMessages()) {
                streamIds.addAll(messageResult.getStreamIds());
                usedInputIds.put(messageResult.getSourceNodeId(), messageResult.getSourceInputId());
                if (messageResult.getSourceRadioId() != null) {
                    usedRadioIds.put(messageResult.getSourceRadioId(), messageResult.getSourceRadioInputId());
                }
            }
            // resolve all stream information in the result set
            final HashSet<Stream> allStreams = Sets.newHashSet(streamService.all().iterator());
            streams = Sets.newHashSet(Collections2.transform(Sets.filter(allStreams, new Predicate<Stream>() {
                @Override
                public boolean apply(Stream input) {
                    return streamIds.contains(input.getId());
                }
            }), new Function<Stream, StreamDescription>() {
                @Nullable
                @Override
                public StreamDescription apply(@Nullable Stream stream) {
                    return new StreamDescription(stream);
                }
            }));

            // resolve all used inputs and nodes from the result set
            final Map<String, Node> nodeMap = serverNodes.asMap();
            for (final String nodeId : usedInputIds.keySet()) {
                final HashSet<Input> allInputs = Sets.newHashSet(nodeMap.get(nodeId).getInputs());
                inputs = Sets.newHashSet(Collections2.transform(Sets.filter(allInputs, new Predicate<Input>() {
                    @Override
                    public boolean apply(Input input) {
                        return usedInputIds.get(nodeId).contains(input.getId());
                    }
                }), new Function<Input, InputDescription>() {
                    @Nullable
                    @Override
                    public InputDescription apply(Input input) {
                        return new InputDescription(input);
                    }
                }));
            }

            // resolve radio inputs
            for (final String radioId : usedRadioIds.keySet()) {
                try {
                    final Radio radio = nodeService.loadRadio(radioId);
                    nodes.put(radio.getId(), new NodeDescription(radio));
                    final HashSet<Input> allRadioInputs = Sets.newHashSet(radio.getInputs());
                    inputs.addAll(Collections2.transform(Sets.filter(allRadioInputs, new Predicate<Input>() {
                        @Override
                        public boolean apply(Input input) {
                            return usedRadioIds.get(radioId).contains(input.getId());
                        }
                    }), new Function<Input, InputDescription>() {
                        @Override
                        public InputDescription apply(Input input) {
                            return new InputDescription(input);
                        }
                    }));

                } catch (NodeService.NodeNotFoundException e) {
                    Logger.warn("Could not load radio node " + radioId, e);
                }
            }


            searchResult.setAllFields(getAllFields());

            // histogram resolution (strangely aka interval)
            if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
                interval = determineHistogramResolution(searchResult);
            }
            histogramResult = search.dateHistogram(interval);
            formattedHistogramResults = formatHistogramResults(histogramResult.getResults(), displayWidth);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                try {
                    QueryParseError qpe = objectMapper.readValue(e.getResponseBody(), QueryParseError.class);
                    return ok(views.html.search.queryerror.render(currentUser(), q, qpe, savedSearch, fields, stream));
                } catch (IOException ioe) {
                    // Ignore
                }
            }

            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        return ok(views.html.searchv2.index.render(currentUser(),
                                                   q,
                                                   search,
                                                   page,
                                                   savedSearch,
                                                   selectedFields,
                                                   searchResult,
                                                   histogramResult,
                                                   formattedHistogramResults,
                                                   nodes,
                                                   Maps.uniqueIndex(streams, new Function<StreamDescription, String>() {
                                                       @Nullable
                                                       @Override
                                                       public String apply(@Nullable StreamDescription stream) {
                                                           return stream == null ? null : stream.getId();
                                                       }
                                                   }),
                                                   Maps.uniqueIndex(inputs, new Function<InputDescription, String>() {
                                                       @Nullable
                                                       @Override
                                                       public String apply(@Nullable InputDescription input) {
                                                           return input == null ? null : input.getId();
                                                       }
                                                   }),
                                                   stream));

    }

    protected String determineHistogramResolution(final SearchResult searchResult) {
        final String interval;
        final int HOUR = 60;
        final int DAY = HOUR * 24;
        final int WEEK = DAY * 7;
        final int MONTH = HOUR * 24 * 30;
        final int YEAR = MONTH * 12;

        // Return minute as default resolution if search from and to DateTimes are not available
        if (searchResult.getFromDateTime() == null && searchResult.getToDateTime() == null) {
            return "minute";
        }

        int queryRangeInMinutes;

        // We don't want to use fromDateTime coming from the search query if the user asked for all messages
        if (isEmptyRelativeRange(searchResult.getTimeRange())) {
            List<IndexRangeSummary> usedIndices = searchResult.getUsedIndices();
            Collections.sort(usedIndices, new Comparator<IndexRangeSummary>() {
                @Override
                public int compare(IndexRangeSummary o1, IndexRangeSummary o2) {
                    return o1.start().compareTo(o2.start());
                }
            });
            IndexRangeSummary oldestIndex = usedIndices.get(0);
            queryRangeInMinutes = Minutes.minutesBetween(oldestIndex.start(),
                                                         searchResult.getToDateTime()).getMinutes();
        } else {
            queryRangeInMinutes = Minutes.minutesBetween(searchResult.getFromDateTime(),
                                                         searchResult.getToDateTime()).getMinutes();
        }

        if (queryRangeInMinutes < DAY / 2) {
            interval = "minute";
        } else if (queryRangeInMinutes < DAY * 2) {
            interval = "hour";
        } else if (queryRangeInMinutes < MONTH) {
            interval = "day";
        } else if (queryRangeInMinutes < MONTH * 6) {
            interval = "week";
        } else if (queryRangeInMinutes < YEAR * 2) {
            interval = "month";
        } else if (queryRangeInMinutes < YEAR * 10) {
            interval = "quarter";
        } else {
            interval = "year";
        }
        return interval;
    }

    private boolean isEmptyRelativeRange(TimeRange timeRange) {
        return (timeRange.getType() == TimeRange.Type.RELATIVE) && (((RelativeRange) timeRange).isEmptyRange());
    }

    /**
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     *
     * @return A JSON string representation of the result, suitable for Rickshaw data graphing.
     */
    protected String formatHistogramResults(Map<String, Long> histogramResults, int displayWidth) {
        final int saneDisplayWidth = (displayWidth == -1 || displayWidth < 100 || displayWidth > DEFAULT_ASSUMED_GRAPH_RESOLUTION) ? DEFAULT_ASSUMED_GRAPH_RESOLUTION : displayWidth;
        final List<Map<String, Long>> points = Lists.newArrayList();

        // using the absolute value guarantees, that there will always be enough values for the given resolution
        final int factor = (saneDisplayWidth != -1 && histogramResults.size() > saneDisplayWidth) ? histogramResults.size() / saneDisplayWidth : 1;

        int index = 0;
        for (Map.Entry<String, Long> result : histogramResults.entrySet()) {
            // TODO: instead of sampling we might consider interpolation (compare DashboardsApiController)
            if (index % factor == 0) {
                Map<String, Long> point = Maps.newHashMap();
                point.put("x", Long.parseLong(result.getKey()));
                point.put("y", result.getValue());

                points.add(point);
            }
            index++;
        }

        return Json.stringify(Json.toJson(points));
    }

    protected Set<String> getSelectedFields(String fields) {
        Set<String> selectedFields = Sets.newLinkedHashSet();
        if (fields != null && !fields.isEmpty()) {
            Iterables.addAll(selectedFields, Splitter.on(',').split(fields));
        } else {
            selectedFields.addAll(Field.STANDARD_SELECTED_FIELDS);
        }
        return selectedFields;
    }

    public Result exportAsCsv(String q,
                              String filter,
                              String rangeType,
                              int relative,
                              String from,
                              String to,
                              String keyword,
                              String fields) {
        UniversalSearch search;
        try {
            search = getSearch(q,
                               filter.isEmpty() ? null : filter,
                               rangeType,
                               relative,
                               from,
                               to,
                               keyword,
                               0,
                               UniversalSearch.DEFAULT_SORT);
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        final InputStream stream;
        try {
            Set<String> selectedFields = getSelectedFields(fields);
            stream = search.searchAsCsv(selectedFields);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        response().setContentType(MediaType.CSV_UTF_8.toString());
        response().setHeader("Content-Disposition", "attachment; filename=graylog-searchresult.csv");
        return ok(stream);
    }

    protected List<Field> getAllFields() {
        List<Field> allFields = Lists.newArrayList();
        for (String f : messagesService.getMessageFields()) {
            allFields.add(new Field(f));
        }
        return allFields;
    }

    protected UniversalSearch getSearch(String q,
                                        String filter,
                                        String rangeType,
                                        int relative,
                                        String from,
                                        String to,
                                        String keyword,
                                        int page,
                                        SearchSort order)
            throws InvalidRangeParametersException, IllegalArgumentException {
        if (q == null || q.trim().isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange = TimeRange.factory(rangeType, relative, from, to, keyword);

        UniversalSearch search;
        if (filter == null) {
            search = searchFactory.queryWithRangePageAndOrder(q, timerange, page, order);
        } else {
            search = searchFactory.queryWithFilterRangePageAndOrder(q, filter, timerange, page, order);
        }

        return search;
    }

    protected SearchSort buildSearchSort(String sortField, String sortOrder) {
        if (sortField == null || sortOrder == null || sortField.isEmpty() || sortOrder.isEmpty()) {
            return UniversalSearch.DEFAULT_SORT;
        }

        try {
            return new SearchSort(sortField, SearchSort.Direction.valueOf(sortOrder.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return UniversalSearch.DEFAULT_SORT;
        }
    }


    public static class InputDescription {
        @JsonIgnore
        private final Input input;

        public InputDescription(Input input) {
            this.input = input;
        }

        @JsonProperty
        public String getId() {
            return input.getId();
        }

        @JsonProperty
        public String getName() {
            return input.getName();
        }

        @JsonProperty
        public String getTitle() {
            return input.getTitle();
        }

        @JsonProperty
        public String getCreatorUser() {
            return input.getCreatorUser().getName();
        }

        @JsonProperty
        public Boolean getGlobal() {
            return input.getGlobal();
        }

        @JsonProperty
        public DateTime getCreatedAt() {
            return input.getCreatedAt();
        }

        @JsonProperty
        public String getType() {
            return input.getType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InputDescription that = (InputDescription) o;

            return input.equals(that.input);

        }

        @Override
        public int hashCode() {
            return input.hashCode();
        }
    }

    public static class StreamDescription {
        @JsonIgnore
        private final Stream stream;

        public StreamDescription(Stream stream) {
            this.stream = stream;
        }

        @JsonProperty
        public String getDescription() {
            return stream.getDescription();
        }

        @JsonProperty
        public String getTitle() {
            return stream.getTitle();
        }

        @JsonProperty
        public String getCreatorUser() {
            return stream.getCreatorUser().getName();
        }

        @JsonProperty
        public DateTime getCreatedAt() {
            return stream.getCreatedAt();
        }

        @JsonProperty
        public String getId() {
            return stream.getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StreamDescription that = (StreamDescription) o;

            return stream.equals(that.stream);

        }

        @Override
        public int hashCode() {
            return stream.hashCode();
        }
    }

    public static class NodeDescription {
        @JsonIgnore
        private final ClusterEntity entity;

        public NodeDescription(ClusterEntity cluster) {
            this.entity = cluster;
        }

        @JsonProperty
        public String getNodeId() {
            return entity.getNodeId();
        }

        @JsonProperty
        public String getShortNodeId() {
            return entity.getShortNodeId();
        }

        @JsonProperty
        public String getHostname() {
            return entity.getHostname();
        }

        @JsonProperty
        public boolean isMaster() {
            return entity instanceof Node && ((Node) entity).isMaster();
        }

        @JsonProperty
        public boolean isRadio() {
            return entity instanceof Radio;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeDescription that = (NodeDescription) o;

            return entity.equals(that.entity);

        }

        @Override
        public int hashCode() {
            return entity.hashCode();
        }
    }

    public Result showMessage(String index, String id) {
        final Set<InputDescription> inputs = Sets.newHashSet();
        final Set<StreamDescription> streams = Sets.newHashSet();
        final Set<NodeDescription> nodes = Sets.newHashSet();

        try {
            final MessageResult message = messagesService.getMessage(index, id);
            final Node sourceNode = getSourceNode(message);
            final Radio sourceRadio = getSourceRadio(message);
            final Input sourceInput = getSourceInput(sourceNode, message);
            final Input sourceRadioInput = getSourceInput(sourceRadio, message);

            nodes.add(new NodeDescription(sourceNode));
            if (sourceRadio != null) {
                nodes.add(new NodeDescription(sourceRadio));
            }
            inputs.add(new InputDescription(sourceInput));
            if (sourceRadioInput != null) {
                inputs.add(new InputDescription(sourceRadioInput));
            }

            for (String streamId : message.getStreamIds()) {
                if (isPermitted(RestPermissions.STREAMS_READ, streamId)) {
                    try {
                        final Stream stream = streamService.get(streamId);
                        streams.add(new StreamDescription(stream));
                    } catch (APIException e) {
                        //  We get a 404 if the stream no longer exists.
                        Logger.debug("Skipping stream of message", e);
                    }
                }
            }

            return ok(views.html.searchv2.show_message.render(currentUser(),
                    message,
                    Maps.uniqueIndex(nodes, new Function<NodeDescription, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable NodeDescription node) {
                            return node == null ? null : node.getNodeId();
                        }
                    }),
                    Maps.uniqueIndex(streams, new Function<StreamDescription, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable StreamDescription stream) {
                            return stream == null ? null : stream.getId();
                        }
                    }),
                    Maps.uniqueIndex(inputs, new Function<InputDescription, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable InputDescription input) {
                            return input == null ? null : input.getId();
                        }
                    })));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    private Node getSourceNode(MessageResult m) {
        try {
            return nodeService.loadNode(m.getSourceNodeId());
        } catch (Exception e) {
            Logger.warn("Could not derive source node from message <" + m.getId() + ">.", e);
        }

        return null;
    }

    private Radio getSourceRadio(MessageResult m) {
        if (m.viaRadio()) {
            try {
                return nodeService.loadRadio(m.getSourceRadioId());
            } catch (Exception e) {
                Logger.warn("Could not derive source radio from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Node node, MessageResult m) {
        if (node != null && isPermitted(RestPermissions.INPUTS_READ, m.getSourceInputId())) {
            try {
                return node.getInput(m.getSourceInputId());
            } catch (Exception e) {
                Logger.warn("Could not derive source input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Radio radio, MessageResult m) {
        if (radio != null) {
            try {
                return radio.getInput(m.getSourceRadioInputId());
            } catch (Exception e) {
                Logger.warn("Could not derive source radio input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }
}
