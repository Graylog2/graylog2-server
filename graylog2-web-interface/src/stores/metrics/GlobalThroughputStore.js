import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const GlobalThroughputStore = Reflux.createStore({
  listenables: [],
  throughput: {
    input: 0,
    output: 0,
    loading: false,
  },
  metrics: {
    input: 'org.graylog2.throughput.input.1-sec-rate',
    output: 'org.graylog2.throughput.output.1-sec-rate',
    loading: true,
  },

  init() {
    MetricsActions.addGlobal(this.metrics.input);
    MetricsActions.addGlobal(this.metrics.output);
    this.listenTo(MetricsStore, this.updateMetrics);
    setInterval(MetricsActions.list, this.INTERVAL);
  },

  getInitialState() {
    return { throughput: this.throughput };
  },

  INTERVAL: 2000,
  updateMetrics(update) {
    if (!update.metrics) {
      return;
    }
    Object.keys(update.metrics).forEach((nodeId) => {
      const inputMetric = update.metrics[nodeId][this.metrics.input];
      const outputMetric = update.metrics[nodeId][this.metrics.output];
      if (inputMetric) {
        this.throughput.input += inputMetric.metric.value;
      }
      if (outputMetric) {
        this.throughput.output += outputMetric.metric.value;
      }
    });

    this.trigger({ throughput: this.throughput });
  },
});

export default GlobalThroughputStore;
