import moment from 'moment';

const findBucketMetrics = (buckets, key, defaultValue = 0) => {
  const rawValue = buckets.find(b => b[key]);
  if (!rawValue || !rawValue[key]) {
    return defaultValue;
  }
  return rawValue[key];
};

export default (data) => {
  const result = {};
  if (!data || !data[0]) {
    return {};
  }
  const { timerange, buckets } = data[0].groups[0];
  buckets
    .map(bucket => ({ key: bucket.key, value: findBucketMetrics(bucket.metrics, 'sum') / findBucketMetrics(bucket.metrics, 'count', 1) }))
    .forEach((b) => {
      const time = moment(b.key).unix();
      result[time] = b.value;
    });
  return {
    results: result,
    timerange: timerange,
  };
};
