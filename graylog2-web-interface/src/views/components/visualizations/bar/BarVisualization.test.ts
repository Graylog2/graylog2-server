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
import Pivot, { DateType, ValuesType } from 'views/logic/aggregationbuilder/Pivot';

import { defineSingleDateBarWidth } from './BarVisualization';

const timeRangeFrom = '2026-05-12T14:00:00.000Z';
const timeRangeTo = '2026-05-12T15:00:00.000Z';

const timePivot = Pivot.create(['timestamp'], DateType, { interval: { type: 'auto', scaling: 1 } });
const timePivotConfig = AggregationWidgetConfig.builder().rowPivots([timePivot]).build();

const bar = (name: string, x: string[]) => ({
  type: 'bar',
  name,
  originalName: name,
  x,
  y: x.map(() => 1),
});

describe('defineSingleDateBarWidth', () => {
  it('returns chart data unchanged when there is no time row pivot', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.create(['action'], ValuesType, { limit: 15, skip_empty_values: false })])
      .build();
    const chartDataResult = [bar('a', ['2026-05-12T14:30:00.000Z'])];

    expect(defineSingleDateBarWidth(chartDataResult, config, timeRangeFrom, timeRangeTo)).toEqual(chartDataResult);
  });

  it('widens bars when the entire chart has only a single timestamp', () => {
    const chartDataResult = [bar('a', ['2026-05-12T14:30:00.000Z']), bar('b', ['2026-05-12T14:30:00.000Z'])];

    const result = defineSingleDateBarWidth(chartDataResult, timePivotConfig, timeRangeFrom, timeRangeTo);

    expect(result[0]).toHaveProperty('width');
    expect(result[1]).toHaveProperty('width');
  });

  it('does not widen bars in a stacked chart where only some series have a single timestamp', () => {
    const chartDataResult = [
      bar('multi-a', ['2026-05-12T14:15:00.000Z', '2026-05-12T14:30:00.000Z']),
      bar('multi-b', ['2026-05-12T14:15:00.000Z', '2026-05-12T14:30:00.000Z']),
      bar('single-a', ['2026-05-12T14:30:00.000Z']),
      bar('single-b', ['2026-05-12T14:30:00.000Z']),
    ];

    const result = defineSingleDateBarWidth(chartDataResult, timePivotConfig, timeRangeFrom, timeRangeTo);

    result.forEach((data) => {
      expect(data).not.toHaveProperty('width');
    });
  });

  it('ignores non-bar traces when deciding whether the chart spans a single timestamp', () => {
    const scatter = {
      type: 'scatter',
      name: 'Alerts',
      originalName: 'Alerts',
      x: ['2026-05-12T14:36:00.000Z'],
      y: [0],
    };
    const chartDataResult = [bar('a', ['2026-05-12T14:30:00.000Z']), bar('b', ['2026-05-12T14:30:00.000Z']), scatter];

    const result = defineSingleDateBarWidth(chartDataResult, timePivotConfig, timeRangeFrom, timeRangeTo);

    expect(result[0]).toHaveProperty('width');
    expect(result[1]).toHaveProperty('width');
    expect(result[2]).not.toHaveProperty('width');
  });
});
