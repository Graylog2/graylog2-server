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
import { theme } from 'views/components/visualizations/utils/__tests__/fixtures';

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

const mockMapperAxisNumber = {
  'count(foo)': 1,
};

describe('getThresholdShapes', () => {
  it('should generate shapes for thresholds', () => {
    const shapes = getThresholdShapes({
      series: mockSeries,
      widgetUnits: mockWidgetUnits,
      fieldNameToAxisNameMapper: mockFieldNameToAxisNameMapper,
      mapperAxisNumber: mockMapperAxisNumber,
      theme,
    });

    expect(shapes).toEqual({
      annotations: [
        {
          'align': 'left',
          'bgcolor': '#fff000',
          'bordercolor': '#fff000',
          'borderpad': 3,
          'font': {
            'color': '#fff000',
            'size': 12,
          },
          'showarrow': false,
          'text': 'Warning (10 s)',
          'x': 0,
          'xanchor': 'left',
          'xref': 'paper',
          'xshift': 0,
          'y': 10,
          'yanchor': 'bottom',
          'yref': 'y1',
          'yshift': 0,
        },
        {
          'align': 'left',
          'bgcolor': '#ff0000',
          'bordercolor': '#ff0000',
          'borderpad': 3,
          'font': {
            'color': '#ff0000',
            'size': 12,
          },
          'showarrow': false,
          'text': 'Critical (20 s)',
          'x': 0,
          'xanchor': 'left',
          'xref': 'paper',
          'xshift': 0,
          'y': 20,
          'yanchor': 'bottom',
          'yref': 'y1',
          'yshift': 0,
        },
      ],
      'shapes': [
        {
          'line': {
            'color': '#fff000',
          },
          'name': 'Warning',
          'type': 'line',
          'x0': 0,
          'x1': 1,
          'xref': 'paper',
          'y0': 10,
          'y1': 10,
          'yref': 'y1',
        },
        {
          'line': {
            'color': '#ff0000',
          },
          'name': 'Critical',
          'type': 'line',
          'x0': 0,
          'x1': 1,
          'xref': 'paper',
          'y0': 20,
          'y1': 20,
          'yref': 'y1',
        },
      ],
    });
  });

  it('returns empty array when series is undefined', () => {
    expect(
      getThresholdShapes({
        series: undefined,
        widgetUnits: mockWidgetUnits,
        fieldNameToAxisNameMapper: mockFieldNameToAxisNameMapper,
        mapperAxisNumber: mockMapperAxisNumber,
        theme,
      }),
    ).toEqual({ annotations: [], 'shapes': [] });
  });

  it('returns empty array when thresholds are missing', () => {
    expect(
      getThresholdShapes({
        series: [series],
        widgetUnits: mockWidgetUnits,
        fieldNameToAxisNameMapper: mockFieldNameToAxisNameMapper,
        mapperAxisNumber: mockMapperAxisNumber,
        theme,
      }),
    ).toEqual({ annotations: [], 'shapes': [] });
  });
});
