// @flow strict
import type { NodeMetric } from 'stores/metrics/MetricsStore';

const MetricsExtractor = {
  /*
   * Returns an object containing a short name and the metric value for it.
   * Short names are the keys of the metricNames object, which should look like:
   * { shortName: "metricFullName" }
   */
  getValuesForNode(nodeMetrics: NodeMetric, metricNames: { [string]: string }): { [string]: ?number } {
    if (nodeMetrics === null || nodeMetrics === undefined || Object.keys(nodeMetrics).length === 0) {
      return {};
    }

    const metrics = {};
    Object.keys(metricNames).forEach((metricShortName) => {
      const metricFullName = metricNames[metricShortName];
      const metricObject = nodeMetrics[metricFullName];
      if (metricObject) {
        if (metricObject.type === 'gauge') {
          metrics[metricShortName] = metricObject.metric.value;
        } else if (metricObject.type === 'counter') {
          metrics[metricShortName] = metricObject.metric.count;
        } else if (metricObject.type === 'meter') {
          metrics[metricShortName] = metricObject.metric.rate.total;
        } else if (metricObject.type === 'timer') {
          metrics[metricShortName] = metricObject.metric.rate.total;
        } else {
          metrics[metricShortName] = null;
        }
      }
    });

    return metrics;
  },
};

export default MetricsExtractor;
