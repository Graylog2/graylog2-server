'use strict';

var MetricsStore = require('./MetricsStore');

var metricsStore = new MetricsStore();
// we need a way for legacy js code to access this, so it's global.
window['metrics'] = metricsStore;

metricsStore.connect();


// subscribes on the metrics updates across all nodes (nodeId: null). they get pushed once per second.
metricsStore.listen({
    nodeId: null,
    metricNames: ["org.graylog2.throughput.input.1-sec-rate", "org.graylog2.throughput.output.1-sec-rate"],
    callback: function(update) { console.log(update); }
});
