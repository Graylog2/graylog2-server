export const generateSeries = (config, results, chartType) => {
  const x = results.map(v => config.rowPivots.map(p => v[p]).join('-'));
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
