const useAggregationFunctions = jest.fn(() => ({
  count: { type: 'count', description: 'Count' },
  min: { type: 'min', description: 'Minimum' },
  max: { type: 'max', description: 'Maximum' },
  percentile: { type: 'percentile', description: 'Percentile' },
}));

export default useAggregationFunctions;
