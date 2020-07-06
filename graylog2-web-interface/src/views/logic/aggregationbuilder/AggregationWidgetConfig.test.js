// @flow strict
import AggregationWidgetConfig from './AggregationWidgetConfig';
import Series from './Series';
import SortConfig from './SortConfig';

describe('AggregationWidgetConfig', () => {
  it('enables rollups if no column pivots are present', () => {
    const config = AggregationWidgetConfig.builder()
      .columnPivots([])
      .rollup(false)
      .build();

    expect(config.rollup).toEqual(true);
  });

  it('filters sorts referencing nonpresent metrics', () => {
    const config = AggregationWidgetConfig.builder()
      .series([Series.forFunction('count()')])
      .sort([SortConfig.fromSeries(Series.forFunction('avg(field1)'))])
      .build();

    expect(config.sort).toEqual([]);
  });
});
