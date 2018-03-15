const seriesRegex = /^(\w+)\((\w*)\)$/;

const _parseSeries = (s) => {
  const result = seriesRegex.exec(s);
  const definition = {
    type: result[1],
  };
  if (result[2] !== '') {
    definition.field = result[2];
  }
  return definition;
};

const _formatBucket = (fieldNames, series, buckets) => {
  if (fieldNames.length === 0) {
    return [];
  }
  const fieldName = fieldNames.shift();
  return buckets.map((bucket) => {
    const result = {};
    series.forEach((seriesName, idx) => {
      const { type, field } = _parseSeries(seriesName);
      if (bucket.metrics[idx] && bucket.metrics[idx][type]) {
        result[seriesName] = bucket.metrics[idx][type];
      }
    });
    result[fieldName] = bucket.key;
    if (fieldNames.length > 0 && bucket.groups[0]) {
      result[fieldNames[0]] = _formatBucket(fieldNames.slice(), series, bucket.groups[0].buckets);
    }
    return result;
  });
};

export default (data, widget) => {
  const { rowPivots, series } = widget.config;
  if (data && data[0] && data[0].groups[0]) {
    const buckets = _formatBucket(rowPivots.slice(), series, data[0].groups[0].buckets);
    return [{ results: buckets }];
  }
  return [];
};
