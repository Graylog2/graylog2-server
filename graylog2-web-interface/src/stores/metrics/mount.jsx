'use strict';

var MetricsStore = require('./MetricsStore');

var metricsStore = new MetricsStore();
// we need a way for legacy js code to access this, so it's global.
window['metrics'] = metricsStore;

metricsStore.connect();
