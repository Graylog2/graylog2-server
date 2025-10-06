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

import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';

import getThresholdShapes from './getThresholdShapes';

const series = Series.create('count', 'foo');

const seriesWithThresholds = series
  .toBuilder()
  .config(
    SeriesConfig.empty()
      .toBuilder()
      .thresholds([
        { color: '#fff000', value: 10000, name: 'Warning' },
        { color: '#ff0000', value: 20000, name: 'Critical' },
      ])
      .build(),
  )
  .build();

const mockSeries = [seriesWithThresholds];

const mockWidgetUnits = new UnitsConfig({ foo: new FieldUnit('time', 'ms') });

const mockFieldNameToAxisNameMapper = {
  foo: 'y1',
};

describe('getThresholdShapes', () => {
  it('should generate shapes for thresholds', () => {
    const shapes = getThresholdShapes(mockSeries, mockWidgetUnits, mockFieldNameToAxisNameMapper);

    expect(shapes).toEqual([
      {
        type: 'line',
        y0: 10,
        y1: 10,
        x0: 0,
        x1: 1,
        xref: 'paper',
        yref: 'y1',
        name: 'Warning',
        line: { color: '#fff000' },
        label: {
          text: 'Warning (10 s)',
          textposition: 'top right',
          font: { color: '#fff000' },
        },
      },
      {
        type: 'line',
        y0: 20,
        y1: 20,
        x0: 0,
        x1: 1,
        xref: 'paper',
        yref: 'y1',
        name: 'Critical',
        line: { color: '#ff0000' },
        label: {
          text: 'Critical (20 s)',
          textposition: 'top right',
          font: { color: '#ff0000' },
        },
      },
    ]);
  });

  it('returns empty array when series is undefined', () => {
    expect(getThresholdShapes(undefined, mockWidgetUnits, mockFieldNameToAxisNameMapper)).toEqual([]);
  });

  it('returns empty array when thresholds are missing', () => {
    expect(getThresholdShapes([series], mockWidgetUnits, mockFieldNameToAxisNameMapper)).toEqual([]);
  });
});
