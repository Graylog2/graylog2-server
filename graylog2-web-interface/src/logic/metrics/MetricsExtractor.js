const MetricsExtractor = {
  /*
   * Returns an object containing a short name and the metric value for it.
   * Short names are the keys of the metricNames object, which should look like:
   * { shortName: "metricFullName" }
   */
  getValuesForNode(nodeMetrics, metricNames) {
    if (nodeMetrics === null || nodeMetrics === undefined || Object.keys(nodeMetrics).length === 0) {
      return {};
    }

    const metrics = {};
    Object.keys(metricNames).forEach(metricShortName => {
      const metricFullName = metricNames[metricShortName];
      const metricObject = nodeMetrics[metricFullName];
      if (metricObject) {
        metrics[metricShortName] = metricObject.metric.value;
      }
    });

    return metrics;
  },
};

export default MetricsExtractor;