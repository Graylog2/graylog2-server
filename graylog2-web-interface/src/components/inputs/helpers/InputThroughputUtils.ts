import numeral from 'numeral';

export const getValueFromMetric = (metric) => {
  if (metric === null || metric === undefined) {
    return undefined;
  }

  switch (metric.type) {
    case 'meter':
      return metric.metric.rate.mean;
    case 'gauge':
      return metric.metric.value;
    case 'counter':
      return metric.metric.count;
    default:
      return undefined;
  }
};
export const formatCount = (count: number) => numeral(count).format('0,0');
const inputsMeticNames = [
  'incomingMessages',
  'emptyMessages',
  'open_connections',
  'total_connections',
  'written_bytes_1sec',
  'written_bytes_total',
  'read_bytes_1sec',
  'read_bytes_total',
];

export const prefixMetric = (input: { type: string; id: string }, metric: string) => {
  return `${input.type}.${input.id}.${metric}`;
};

export const getMetricNamesForInput = (input: { type: string; id: string }) => {
  return inputsMeticNames.map((metric) => prefixMetric(input, metric));
};

export const calculateInputMetrics = (input: { type: string; id: string }, metrics: Record<string, any>) => {
  const result: Record<string, number> = {};
  const metricNames = getMetricNamesForInput(input);

  metricNames.forEach((metricName) => {
    result[metricName] = Object.keys(metrics).reduce((previous, nodeId) => {
      if (!metrics[nodeId][metricName]) {
        return previous;
      }

      const value = getValueFromMetric(metrics[nodeId][metricName]);

      if (value !== undefined) {
        return isNaN(previous) ? value : previous + value;
      }

      return previous;
    }, NaN);
  });

  return result;
};
