import Reflux from 'reflux';

import MetricsStore from 'stores/metrics/MetricsStore';
import NodesStore from 'stores/nodes/NodesStore';

import MetricsActions from 'actions/metrics/MetricsActions';

const GlobalThroughputStore = Reflux.createStore({
  listenables: [],
  metrics: {
    input: 'org.graylog2.throughput.input.1-sec-rate',
    output: 'org.graylog2.throughput.output.1-sec-rate',
  },

  init() {
    this.listenTo(NodesStore, this.updateNodes);
    this.listenTo(MetricsStore, this.updateMetrics);
  },
  updateNodes(update) {
    const nodeIds = Object.keys(update.nodes);
    nodeIds.forEach((nodeId) => {
      MetricsActions.add(nodeId, this.metrics.input);
      MetricsActions.add(nodeId, this.metrics.output);
    });
  },
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

    this.trigger({throughput: throughput});
  },
});

export default GlobalThroughputStore;
