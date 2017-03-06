import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const MetricsStore = StoreProvider.getStore('Metrics');

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

const GlobalThroughputStore = Reflux.createStore({
  listenables: [],
  metrics: {
    input: 'org.graylog2.throughput.input.1-sec-rate',
    output: 'org.graylog2.throughput.output.1-sec-rate',
  },

  init() {
    MetricsActions.addGlobal(this.metrics.input);
    MetricsActions.addGlobal(this.metrics.output);
    this.listenTo(MetricsStore, this.updateMetrics);
    setInterval(MetricsActions.list, this.INTERVAL);
  },
  INTERVAL: 2000,
  updateMetrics(update) {
    if (!update.metrics) {
      return;
    }
    const throughput = {
      input: 0,
      output: 0,
    };
    Object.keys(update.metrics).forEach((nodeId) => {
      const inputMetric = update.metrics[nodeId][this.metrics.input];
      const outputMetric = update.metrics[nodeId][this.metrics.output];
      if (inputMetric) {
        throughput.input += inputMetric.metric.value;
      }
      if (outputMetric) {
        throughput.output += outputMetric.metric.value;
      }
    });

    this.trigger({ throughput: throughput });
  },
});

export default GlobalThroughputStore;
