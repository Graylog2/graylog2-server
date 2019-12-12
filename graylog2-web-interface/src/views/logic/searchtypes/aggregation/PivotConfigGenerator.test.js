// @flow strict
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

import PivotConfigGenerator from './PivotConfigGenerator';

describe('PivotConfigGenerator', () => {
  const widgetConfigBuilder = AggregationWidgetConfig.builder()
    .rollup(true)
    .rowPivots([Pivot.create('field', 'type')])
    .columnPivots([])
    .series([Series.forFunction('count')]);

  it('should generate a chart config', () => {
    const widgetConfig = widgetConfigBuilder.build();
    const result = PivotConfigGenerator({ config: widgetConfig });
    expect(result[0].config).toMatchSnapshot({
      id: expect.any(String),
    });
  });

  it('should generate a number config with trend', () => {
    const widgetConfig = widgetConfigBuilder.visualization('numeric')
      .visualizationConfig(
        NumberVisualizationConfig.create(true),
      ).build();
    const result = PivotConfigGenerator({ config: widgetConfig });
    expect(result).toHaveLength(2);
    expect(result[0]).toMatchSnapshot({
      id: expect.any(String),
    });
    expect(result[1]).toMatchSnapshot({
      id: expect.any(String),
      timerange: {
        id: expect.any(String),
      },
    });
    expect(result[1].timerange.id).toEqual(result[0].id);
  });

  it('should add a event annotation config when configured', () => {
    const widgetConfig = widgetConfigBuilder.eventAnnotation(true).build();
    const result = PivotConfigGenerator({ config: widgetConfig });
    expect(result).toHaveLength(2);
    expect(result[0]).toMatchSnapshot({
      id: expect.any(String),
    });
    expect(result[1]).toMatchSnapshot({
      id: expect.any(String),
    });
  });
});
