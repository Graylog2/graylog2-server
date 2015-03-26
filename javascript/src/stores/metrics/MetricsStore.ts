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

interface MetricUpdateCallback {
    (update: MetricUpdate):void;
}

interface ListenRequest {
    nodeId: string;
    metricNames: Array<string>;
    callback: MetricUpdateCallback;

}

interface Callback {
    callback: MetricUpdateCallback;
    names: {}
}

class MetricsStore {
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

//            this.sock.send(JSON.stringify({command:"metrics_subscribe", metrics:["org.graylog2.throughput"]}));
        };

        this.sock.onmessage = (e) => {
            var update: MetricUpdate = JSON.parse(e.data);

            this.callbacks.forEach((cb) => {
                // only pass the metrics this caller has asked for, not the entire set
                var interestingMetrics = update.values.filter((metric) => cb.names.hasOwnProperty(metric.name));

                cb.callback({node_id: update.node_id, values: interestingMetrics});
            });
            //console.log('message', e.data);
        };

        this.sock.onclose = () => {
            this.isOpen = false;
            console.log('sockjs connection closed');
        };
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

        this.callbacks.push({callback: request.callback, names: nameMap});

        this.sock.send(JSON.stringify({"command":"metrics_subscribe", metrics:request.metricNames }));
    }

}

export = MetricsStore;
