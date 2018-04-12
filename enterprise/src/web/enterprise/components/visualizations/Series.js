export const generateSeries = (config, results, chartType) => {
  const fieldNames = config.rowPivots.map(({ field }) => field);
  const x = results.map(v => fieldNames.map(p => v[p]).join('-'));
  return config.series.map((s) => {
    const y = results.map(v => v[s]);
    return {
      type: chartType,
      name: s,
      x: x,
      y: y,
    };
  });
};
