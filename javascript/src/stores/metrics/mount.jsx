'use strict';

var MetricsStore = require('./MetricsStore');

var metricsStore = new MetricsStore();
window['metrics'] = metricsStore;

metricsStore.connect();


metricsStore.listen({
    nodeId: "abcd",
    metricNames: ["org.graylog2.throughput.input.1-sec-rate", "org.graylog2.throughput.output.1-sec-rate"],
    callback: function(update) { console.log(update); }
});
