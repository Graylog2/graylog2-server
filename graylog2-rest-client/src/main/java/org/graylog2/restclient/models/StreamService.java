/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.alerts.Alert;
import org.graylog2.restclient.models.api.requests.outputs.AddOutputRequest;
import org.graylog2.restclient.models.api.requests.streams.CreateStreamRequest;
import org.graylog2.restclient.models.api.requests.streams.TestMatchRequest;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restclient.models.api.responses.alerts.CheckConditionResponse;
import org.graylog2.restclient.models.api.responses.streams.CreateStreamResponse;
import org.graylog2.restclient.models.api.responses.streams.GetStreamsResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamSummaryResponse;
import org.graylog2.restclient.models.api.responses.streams.TestMatchResponse;
import org.graylog2.restclient.models.api.responses.system.OutputSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.OutputsResponse;
import org.graylog2.restclient.models.api.results.StreamsResult;
import org.graylog2.restroutes.generated.StreamResource;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import java.io.IOException;
import java.util.*;

public class StreamService {

    private final ApiClient api;
    private final Stream.Factory streamFactory;
    private final Output.Factory outputFactory;
    private final StreamResource resource = routes.StreamResource();

    @Inject
    private StreamService(ApiClient api, Stream.Factory streamFactory, Output.Factory outputFactory) {
        this.api = api;
        this.streamFactory = streamFactory;
        this.outputFactory = outputFactory;
    }

    public List<Stream> all() throws IOException, APIException {
        GetStreamsResponse r = null;
        r = api.path(resource.get(), GetStreamsResponse.class).execute();

        List<Stream> streams = Lists.newArrayList();

        for (StreamSummaryResponse stream : r.streams) {
            streams.add(streamFactory.fromSummaryResponse(stream));
        }

        //return new StreamsResult(r.total, r.streams);
        return streams;
    }

    public List<Stream> allEnabled() throws IOException, APIException {
        GetStreamsResponse r = null;
        r = api.path(resource.getEnabled(), GetStreamsResponse.class).execute();

        List<Stream> streams = Lists.newArrayList();

        for (StreamSummaryResponse stream : r.streams) {
            streams.add(streamFactory.fromSummaryResponse(stream));
        }

        return streams;
    }

    public Stream get(String streamId) throws IOException, APIException {
        StreamSummaryResponse streamResponse = null;
        streamResponse = api.path(resource.get(streamId), StreamSummaryResponse.class).execute();

        return streamFactory.fromSummaryResponse(streamResponse);
    }

    public String create(CreateStreamRequest request) throws APIException, IOException {
        CreateStreamResponse csr = api.path(resource.create(), CreateStreamResponse.class)
                .body(request).expect(Http.Status.CREATED).execute();
        return csr.streamId;
    }

    public void update(String streamId, CreateStreamRequest request) throws APIException, IOException {
        api.path(resource.update(streamId)).body(request).expect(Http.Status.OK).execute();
    }

    public void delete(String streamId) throws APIException, IOException {
        api.path(resource.delete(streamId)).expect(Http.Status.NO_CONTENT).execute();
    }

    public void pause(String streamId) throws APIException, IOException {
        api.path(resource.pause(streamId)).expect(Http.Status.OK).execute();
    }

    public void resume(String streamId) throws APIException, IOException {
        api.path(resource.resume(streamId)).expect(Http.Status.OK).execute();
    }

    public TestMatchResponse testMatch(String streamId, TestMatchRequest request) throws APIException, IOException {
        TestMatchResponse testMatchResponse = null;
        testMatchResponse = api.path(resource.testMatch(streamId), TestMatchResponse.class)
                .body(request).expect(Http.Status.OK).execute();
        return testMatchResponse;
    }

    public String cloneStream(String streamId, CreateStreamRequest request) throws APIException, IOException {
        CreateStreamResponse csr = api.path(resource.cloneStream(streamId), CreateStreamResponse.class)
                .body(request).expect(Http.Status.CREATED).execute();
        return csr.streamId;
    }

    public void sendDummyAlert(String streamId) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().sendDummyAlert(streamId))
                .expect(Http.Status.NO_CONTENT).execute();
    }

    public List<Alert> allowedAlertsSince(int since) throws IOException, APIException {
        List<Alert> alerts = Lists.newArrayList();

        for(Stream stream : all()) {
            alerts.addAll(stream.getAlertsSince(since));
        }

        return alerts;
    }

    public CheckConditionResponse activeAlerts(String streamId) throws APIException, IOException {
        return api.path(routes.StreamAlertResource().checkConditions(streamId), CheckConditionResponse.class).execute();
    }

    public List<Output> getOutputs(String streamId) throws APIException, IOException {
        OutputsResponse outputsResponse = api.path(routes.StreamOutputResource().get(streamId), OutputsResponse.class).execute();
        List<Output> result = new ArrayList<>();
        for(OutputSummaryResponse response : outputsResponse.outputs)
            result.add(outputFactory.fromSummaryResponse(response));

        return result;
    }

    public void addOutput(String streamId, final String outputId) throws APIException, IOException {
        Set<String> outputs = new HashSet<String>() {
            {
                add(outputId);
            }
        };

        addOutputs(streamId, outputs);
    }

    public void addOutputs(String streamId, Set<String> outputIds) throws APIException, IOException {
        AddOutputRequest request = new AddOutputRequest();
        request.outputs = outputIds;
        api.path(routes.StreamOutputResource().add(streamId)).expect(Http.Status.CREATED).body(request).execute();
    }

    public void removeOutput(String streamId, String outputId) throws APIException, IOException {
        api.path(routes.StreamOutputResource().remove(streamId, outputId)).execute();
    }

    public void removeOutputs(String streamId, Set<String> outputIds) throws APIException, IOException {
        for (String outputId : outputIds)
            removeOutput(streamId, outputId);
    }
}
