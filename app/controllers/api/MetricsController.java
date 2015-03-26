package controllers.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import controllers.AuthenticatedController;
import models.sockjs.CreateSessionCommand;
import models.sockjs.MetricValuesUpdate;
import models.sockjs.SockJsCommand;
import models.sockjs.SubscribeMetricsUpdates;
import models.sockjs.UnsubscribeMetricsUpdates;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ApiRequestBuilder;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.api.requests.MultiMetricRequest;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import play.Logger;
import play.Play;
import play.libs.Crypto;
import play.libs.F;
import play.libs.Json;
import play.sockjs.SockJS;
import play.sockjs.SockJSRouter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.graylog2.restroutes.generated.routes.MetricsResource;

public class MetricsController extends AuthenticatedController {

    private final NodeService nodeService;
    private final ScheduledExecutorService executor;

    private static ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

    @Inject
    public MetricsController(NodeService nodeService) {
        executor = Executors.newSingleThreadScheduledExecutor();
        this.nodeService = nodeService;
    }

    public static SockJSRouter metrics = new SockJSRouter() {

        @Override
        public SockJS sockjs() {

            // this is the dance to get DI working
            final MetricsController controllerInstance =
                    Play.application()
                            .getWrappedApplication()
                            .global()
                            .getControllerInstance(MetricsController.class);

            return new SockJS() {

                private Set<String> subscribedMetrics = Sets.newHashSet();
                private Multimap<String, String> metricsPerNode =
                        Multimaps.synchronizedMultimap(HashMultimap.<String, String>create());

                private String clearSessionId;

                @Override
                public void onReady(final In in, final Out out) {

                    in.onMessage(new F.Callback<String>() {
                        @Override
                        public void invoke(String s) throws Throwable {
                            try {
                                final SockJsCommand command = objectMapper.readValue(s, SockJsCommand.class);
                                if (command instanceof CreateSessionCommand) {

                                    final String sessionId = ((CreateSessionCommand) command).sessionId;

                                    final String userAndSessionId = Crypto.decryptAES(sessionId);
                                    final StringTokenizer tokenizer = new StringTokenizer(userAndSessionId, "\t");
                                    if (tokenizer.countTokens() != 2) {
                                        Logger.warn("Invalid credentials '{}' for sockjs connection, closing socket.",
                                                    userAndSessionId);
                                        out.close();
                                    }
                                    //noinspection unused
                                    String ignored = tokenizer.nextToken();
                                    clearSessionId = tokenizer.nextToken();
                                } else if (command instanceof SubscribeMetricsUpdates) {
                                    final SubscribeMetricsUpdates metricsUpdates = (SubscribeMetricsUpdates) command;
                                    Logger.info("Subscribed to metrics {} on node {}",
                                                metricsUpdates.metrics,
                                                MoreObjects.firstNonNull(metricsUpdates.nodeId, "ALL"));

                                    metricsPerNode.putAll(metricsUpdates.nodeId, metricsUpdates.metrics);
                                } else if (command instanceof UnsubscribeMetricsUpdates) {
                                    for (String metric : ((UnsubscribeMetricsUpdates) command).metrics) {
                                        subscribedMetrics.remove(metric);
                                    }
                                }
                            } catch (Exception e) {
                                Logger.error("Unhandled exception", e);
                            }
                        }
                    });

                    final ScheduledFuture<?> scheduledFuture = controllerInstance.executor.scheduleAtFixedRate(
                            new Runnable() {
                                @Override
                                public void run() {
                                    // don't send anything if not authenticated or nothing is requested
                                    if (clearSessionId == null || metricsPerNode.isEmpty()) {
                                        return;
                                    }
                                    final ApiClient api = controllerInstance.api();

                                    final List<MetricValuesUpdate> valuesPerNode = Lists.newArrayList();

                                    for (String nodeId : metricsPerNode.keySet()) {
                                        try {
                                            final MultiMetricRequest request = new MultiMetricRequest();
                                            request.metrics = metricsPerNode.get(nodeId).toArray(new String[]{});

                                            ApiRequestBuilder<MetricsListResponse> requestBuilder = api.path(MetricsResource().multipleMetrics(), MetricsListResponse.class)
                                                    .body(request)
                                                    .session(clearSessionId)
                                                    .extendSession(false)
                                                    .expect(200);


                                            if (nodeId == null) {
                                                final Map<Node, MetricsListResponse> responseMap = requestBuilder.fromAllNodes().executeOnAll();

                                                for (Map.Entry<Node, MetricsListResponse> perNodeEntry : responseMap.entrySet()) {
                                                    final MetricValuesUpdate valuesUpdate = new MetricValuesUpdate();
                                                    valuesUpdate.nodeId = perNodeEntry.getKey().getNodeId();
                                                    valuesUpdate.values = Lists.newArrayList();
                                                    for (Map.Entry<String, Metric> entry : perNodeEntry.getValue().getMetrics().entrySet()) {
                                                        valuesUpdate.values.add(new MetricValuesUpdate.NamedMetric(entry.getKey(), entry.getValue()));
                                                    }
                                                    valuesPerNode.add(valuesUpdate);
                                                }


                                            } else {
                                                try {
                                                    final Node node = controllerInstance.nodeService.loadNode(nodeId);
                                                    final MetricsListResponse response = requestBuilder.node(node).execute();

                                                    final MetricValuesUpdate valuesUpdate = new MetricValuesUpdate();
                                                    valuesUpdate.nodeId = node.getNodeId();
                                                    valuesUpdate.values = Lists.newArrayList();
                                                    for (Map.Entry<String, Metric> entry : response.getMetrics().entrySet()) {
                                                        valuesUpdate.values.add(new MetricValuesUpdate.NamedMetric(entry.getKey(), entry.getValue()));
                                                    }
                                                    valuesPerNode.add(valuesUpdate);

                                                } catch (NodeService.NodeNotFoundException e) {
                                                    Logger.warn("Unknown node {}, skipping it.", nodeId);
                                                }
                                            }

                                        } catch (APIException | IOException e) {
                                            Logger.warn("Unable to load metrics", e);
                                        }
                                    }
                                    Logger.info(Json.toJson(valuesPerNode).toString());
                                    out.write(Json.toJson(valuesPerNode).toString());

                                }
                            }, 1, 1, TimeUnit.SECONDS);

                    in.onClose(new F.Callback0() {
                        @Override
                        public void invoke() throws Throwable {
                            scheduledFuture.cancel(true);
                        }
                    });

                }
            };
        }
    };

}
