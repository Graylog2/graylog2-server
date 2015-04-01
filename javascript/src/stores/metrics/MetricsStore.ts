/// <reference path="../../../declarations/node/node.d.ts" />
/// <reference path="../../../declarations/sockjs-client/sockjs-client.d.ts" />


'use strict';

// this is global in top.scala.html
declare var gl2UserSessionId: string;

var SockJS = require("sockjs-client");
import UserNotification = require("../../util/UserNotification");
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
        this.sock = new SockJS(this.METRICS_SOCKJS_URL);

        this.sock.onopen = () => {
            this.isOpen = true;

            this.sock.send(JSON.stringify({command:"create_session", sessionId:gl2UserSessionId}));

            // callers where potentially queued when they ran before the sockjs connection had been established.
            // process those first before we continue.
            this.queuedRequests.forEach((request) => this.registerRequest(request));
            this.queuedRequests = [];
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
            // TODO support reconnecting when web interface goes away
            console.log('sockjs connection closed');
        };

        MetricsStore.instance = this;
    }

    listen(request: ListenRequest) {
        if (this.isOpen) {
            this.registerRequest(request);
        } else {
            this.queuedRequests.push(request);
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
