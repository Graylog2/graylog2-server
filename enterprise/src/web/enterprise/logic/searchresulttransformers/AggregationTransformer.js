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

const _formatBucket = (rowPivots, columnPivots, series, buckets) => {
  if (rowPivots.length === 0) {
    return [];
  }
  const fieldName = rowPivots.shift();
  return buckets.map((bucket) => {
    const result = {};
    series.forEach((seriesName, idx) => {
      const { type, field } = _parseSeries(seriesName);
      if (bucket.metrics[idx] && bucket.metrics[idx][type]) {
        result[seriesName] = bucket.metrics[idx][type];
      }
    });
    result[fieldName] = bucket.key;
    if (rowPivots.length > 0 && bucket.groups[0]) {
      result[rowPivots[0]] = _formatBucket(rowPivots.slice(), columnPivots, series, bucket.groups[0].buckets);
    }

    if (rowPivots.length === 0 && columnPivots.length > 0) {
      columnPivots.forEach((columnPivotName, idx) => {
        if (bucket && bucket.groups && bucket.groups[idx]) {
          result[columnPivotName] = _formatBucket([columnPivotName], [], series, bucket.groups[idx].buckets);
        }
      });
    }
    return result;
  });
};

export default (data, widget) => {
  const { columnPivots, rowPivots, series } = widget.config;
  if (data && data[0] && data[0].groups[0]) {
    const buckets = _formatBucket(rowPivots.map(({ field }) => field), columnPivots.map(({ field }) => field), series, data[0].groups[0].buckets);
    return [{ results: buckets }];
  }
  return [];
};
