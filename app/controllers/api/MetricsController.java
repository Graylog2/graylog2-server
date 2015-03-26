package controllers.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import controllers.AuthenticatedController;
import models.sockjs.CreateSessionCommand;
import models.sockjs.MetricValuesUpdate;
import models.sockjs.SockJsCommand;
import models.sockjs.SubscribeMetricsUpdates;
import models.sockjs.UnsubscribeMetricsUpdates;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
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
import play.mvc.Http;
import play.sockjs.CookieCalculator;
import play.sockjs.SockJS;
import play.sockjs.SockJSRouter;

import javax.inject.Inject;
import java.io.IOException;
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

    private static ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

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

                                    Logger.warn("session is {}", clearSessionId);

                                } else if (command instanceof SubscribeMetricsUpdates) {
                                    for (String metric : ((SubscribeMetricsUpdates) command).metrics) {
                                        Logger.warn("Subscribed to metric {}", metric);
                                        subscribedMetrics.add(metric);
                                    }
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
                                    if (clearSessionId == null || subscribedMetrics.isEmpty()) {
                                        return;
                                    }
                                    try {
                                        final ApiClient api = controllerInstance.api();

                                        final Node node = controllerInstance.nodeService.loadMasterNode();

                                        final MultiMetricRequest request = new MultiMetricRequest();
                                        request.metrics = subscribedMetrics.toArray(new String[subscribedMetrics.size()]);

                                        MetricsListResponse response = api.path(MetricsResource().multipleMetrics(), MetricsListResponse.class)
                                                .node(node)
                                                .body(request)
                                                .session(clearSessionId)
                                                .extendSession(false)
                                                .expect(200)
                                                .execute();

                                        final MetricValuesUpdate valuesUpdate = new MetricValuesUpdate();
                                        valuesUpdate.nodeId = node.getNodeId();
                                        valuesUpdate.values = Lists.newArrayList();
                                        for (Map.Entry<String, Metric> entry : response.getMetrics().entrySet()) {
                                            valuesUpdate.values.add(new MetricValuesUpdate.NamedMetric(entry.getKey(), entry.getValue()));
                                        }

                                        out.write(Json.toJson(valuesUpdate).toString());
                                    } catch (APIException | IOException e) {
                                        Logger.warn("Unable to load metrics", e);
                                    }
                                }
                            }, 1, 1, TimeUnit.SECONDS);

                    in.onClose(new F.Callback0() {
                        @Override
                        public void invoke() throws Throwable {
                            scheduledFuture.cancel(true);
                            Logger.info("Good bye.");
                        }
                    });

                }
            };
        }
    };

    public static class Graylog2Cookie implements CookieCalculator {
        @Override
        public Http.Cookie cookie(Http.RequestHeader request) {
            return null;
        }
    }
}
