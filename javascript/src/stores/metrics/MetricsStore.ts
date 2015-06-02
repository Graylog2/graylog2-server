/// <reference path="../../../declarations/node/node.d.ts" />
/// <reference path="../../../declarations/sockjs-client/sockjs-client.d.ts" />

'use strict';

// this is global in top.scala.html
declare var gl2UserSessionId: string;
declare var sockJsWebSocketsEnabled: boolean;

var SockJS = require("sockjs-client");
import URLUtils = require("../../util/URLUtils");


interface NamedMetric {
    name: string;
    value: any;
}

interface MetricUpdate {
    node_id: string;
    values: Array<NamedMetric>;
}

interface MetricResponse {
    metrics: Array<MetricUpdate>;
    has_error: boolean;
}

interface MetricUpdateCallback {
    (update: Array<MetricUpdate>, hasError: boolean):void;
}

interface ListenRequest {
    nodeId: string;
    metricNames: Array<string>;
    callback: MetricUpdateCallback;
}

interface Callback {
    callback: MetricUpdateCallback;
    nodeId: String;
    names: {};
}

class MetricsStore {
    public static instance: MetricsStore = null;

    private METRICS_SOCKJS_URL: string = URLUtils.appPrefixed('/a/metrics');

    private sock: SockJS;

    private callbacks: Array<Callback> = [];

    private isOpen: Boolean = false;

    private queuedRequests: Array<ListenRequest> = [];

    connect() {
        if (sockJsWebSocketsEnabled) {
            this.sock = new SockJS(this.METRICS_SOCKJS_URL);
        } else {
            // only allow a subset of transport types.
            this.sock = new SockJS(this.METRICS_SOCKJS_URL,
                null, /* reserved param */
                { transports: ['xhr-polling', 'xdr-polling', 'iframe-xhr-polling', 'jsonp-polling']});
        }

        this.sock.onopen = () => {
            this.isOpen = true;

            this.sock.send(JSON.stringify({command:"create_session", sessionId:gl2UserSessionId}));

            // callers where potentially queued when they ran before the sockjs connection had been established.
            // process those first before we continue
            // in case we have to reconnect (because the web interface process went away, we want to re-register, too.
            this.queuedRequests.forEach((request) => this.registerRequest(request));
        };

        this.sock.onmessage = (e) => {
            var update: MetricResponse = JSON.parse(e.data);

            this.callbacks.forEach((cb) => {
                var updates: Array<MetricUpdate> = [];
                if (update.has_error) {
                    // there was a server side error, signal the callback
                    cb.callback([], true);
                } else if (cb.nodeId === null) {
                    // caller wanted all nodes
                    update.metrics.forEach((nodeMetrics) => {
                        var interestingMetrics = nodeMetrics.values.filter((metric) => cb.names.hasOwnProperty(metric.name));
                        updates.push({node_id: nodeMetrics.node_id, values: interestingMetrics});
                    });
                } else {
                    // caller specified a single node, so we only pass that specific one
                    // only pass the metrics this caller has asked for, not the entire set
                    var nodeMetrics = update.metrics.filter((node) => node.node_id === cb.nodeId);
                    if (nodeMetrics.length === 1) {
                        var interestingMetrics = nodeMetrics[0].values.filter((metric) => cb.names.hasOwnProperty(metric.name));
                        updates.push({node_id: nodeMetrics[0].node_id, values: interestingMetrics});
                    }
                }

                if (updates.length > 0) {
                    cb.callback(updates, false);
                }
            });
        };

        this.sock.onclose = () => {
            this.isOpen = false;
            // notify all callbacks about the error
                this.callbacks.forEach((cb) => {
                    try {
                        cb.callback([], true);
                    } catch (ignore) {}
                });
            // reconnect after two seconds
            setTimeout(() => this.connect(), 2000);
        };

        MetricsStore.instance = this;
    }

    listen(request: ListenRequest) {
        this.queuedRequests.push(request);
        if (this.isOpen) {
            this.registerRequest(request);
        }
    }

    registerRequest(request: ListenRequest) {
        var nameMap = {};
        request.metricNames.forEach((name) => nameMap[name] = 1);

        this.callbacks.push({callback: request.callback, names: nameMap, nodeId: request.nodeId});

        this.sock.send(JSON.stringify({"command":"metrics_subscribe", metrics:request.metricNames, nodeId: request.nodeId }));
    }

}

export = MetricsStore;
