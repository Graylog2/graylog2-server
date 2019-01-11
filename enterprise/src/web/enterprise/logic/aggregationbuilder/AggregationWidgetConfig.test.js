// @flow strict
import AggregationWidgetConfig from './AggregationWidgetConfig';

describe('AggregationWidgetConfig', () => {
  it('enables rollups if no column pivots are present', () => {
    const config = AggregationWidgetConfig.builder()
      .columnPivots([])
      .rollup(false)
      .build();

    expect(config.rollup).toEqual(true);
  });
});