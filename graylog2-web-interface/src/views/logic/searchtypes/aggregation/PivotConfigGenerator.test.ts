/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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

    const [{ id }, { timerange }] = result;

    if (!timerange) {
      throw new Error('Expected `timerange` on generated config, but not present!');
    }

    expect(timerange.id).toEqual(id);
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

  describe('maps time units for time pivots with timeunit intervals', () => {
    const createWidgetConfigWithPivot = (pivot) => AggregationWidgetConfig.builder()
      .rollup(true)
      .rowPivots([pivot])
      .columnPivots([])
      .series([Series.forFunction('count')])
      .build();

    const generateConfigForPivotWithTimeUnit = ({ timeUnit, expectedMappedTimeUnit }) => {
      const config = createWidgetConfigWithPivot(Pivot.create(
        'foo',
        'time',
        { interval: { type: 'timeunit', unit: timeUnit, value: 1 } },
      ));
      const result = PivotConfigGenerator({ config });
      const [{ config: pivotConfig }] = result;

      if (!pivotConfig) {
        throw new Error('Expected `config` in first element of result is missing!');
      }

      const { row_groups: [pivot] } = pivotConfig;
      const { interval: { timeunit } } = pivot;

      expect(timeunit).toEqual(expectedMappedTimeUnit);
    };

    it.each`
    timeUnit      | expectedMappedTimeUnit
    ${'seconds'}  | ${'1s'}
    ${'minutes'}  | ${'1m'}
    ${'hours'}    | ${'1h'}
    ${'days'}     | ${'1d'}
    ${'weeks'}    | ${'1w'}
    ${'months'}   | ${'1M'}
  `('maps time unit $timeUnit to short name $expectedMappedTimeUnit', generateConfigForPivotWithTimeUnit);
  });
});
