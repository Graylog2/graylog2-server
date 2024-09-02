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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { act } from '@testing-library/react-hooks';

import useFeature from 'hooks/useFeature';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import { asMock } from 'helpers/mocking';
import useChartDataSettingsWithCustomUnits
  from 'views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import useBarChartDataSettingsWithCustomUnits
  from 'views/components/visualizations/hooks/useBarChartDataSettingsWithCustomUnits';
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import * as chartLayoutGenerators from 'views/components/visualizations/utils/chartLayoutGenerators';
import type { MappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';

jest.mock('views/components/visualizations/hooks/useChartDataSettingsWithCustomUnits');
jest.mock('hooks/useFeature');
jest.mock('views/components/visualizations/hooks/useWidgetUnits');
jest.mock('views/components/visualizations/utils/getFieldNameFromTrace');
jest.mock('views/components/visualizations/utils/chartLayoutGenerators');

const testConfig: AggregationWidgetConfig = AggregationWidgetConfig.builder().series([
  Series.create('avg', 'fieldTime')
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name('Name1').build()).build(),
  Series.create('avg', 'fieldSize')
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name('Name2').build()).build(),
  Series.create('avg', 'fieldPercent')
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name('Name3').build()).build(),
  Series.create('count'),
]).build();

const units: UnitsConfig = UnitsConfig
  .empty().toBuilder()
  .setFieldUnit('fieldTime', new FieldUnit('time', 'ms'))
  .setFieldUnit('fieldSize', new FieldUnit('size', 'kb'))
  .setFieldUnit('fieldPercent', new FieldUnit('percent', '%'))
  .build();

describe('useBarChartDataSettingsWithCustomUnits', () => {
  beforeEach(() => {
    asMock(useChartDataSettingsWithCustomUnits).mockReturnValue(jest.fn(() => ({
      y: [1, 2, 3],
      yaxis: 'y1',
    })));

    asMock(useFeature).mockReturnValue(true);
    asMock(useWidgetUnits).mockReturnValue(units);
    asMock(getFieldNameFromTrace).mockReturnValue('fieldTime');

    const mappers: MappersForYAxis = {
      fieldNameToAxisCountMapper: {
        fieldTime: 1,
        fieldSize: 2,
        fieldPercent: 3,
        no_field_name_series: 4,
      },
      unitTypeMapper: {
        time: {
          axisCount: 1,
          axisKeyName: 'yaxis',
        },
        size: {
          axisCount: 2,
          axisKeyName: 'yaxis2',
        },
        percent: {
          axisCount: 3,
          axisKeyName: 'yaxis3',
        },
        withoutUnit: {
          axisCount: 4,
          axisKeyName: 'yaxis4',
        },
      },
      yAxisMapper: {},
      mapperAxisNumber: {},
      fieldNameToAxisNameMapper: {},
      seriesUnitMapper: {},
    };
    asMock(chartLayoutGenerators.generateMappersForYAxis).mockReturnValue(mappers);

    asMock(chartLayoutGenerators.getBarChartTraceOffsetSettings).mockReturnValue({
      offsetgroup: 1,
      width: 0.25,
      offset: -0.375,
    });
  });

  it('Runs all related functions and return combined result from them', async () => {
    const { result } = renderHook(() => useBarChartDataSettingsWithCustomUnits(
      {
        config: testConfig,
        barmode: 'group',
        effectiveTimerange: {
          from: '2024-08-11T14:56:10.000Z',
          to: '2024-08-12T15:01:10.000Z',
          type: 'absolute',
        },
      },
    ));

    let barChartDataSettingsWithCustomUnits;

    act(() => {
      barChartDataSettingsWithCustomUnits = result.current(
        { originalName: 'Name1', name: 'Name1', fullPath: 'Name1', values: [1000, 2000, 3000], idx: 1, total: 4, xAxisItemsLength: 10 },
      );
    });

    expect(chartLayoutGenerators.generateMappersForYAxis).toHaveBeenCalledWith({
      series: testConfig.series,
      units,
    });

    expect(useChartDataSettingsWithCustomUnits).toHaveBeenCalledWith({ config: testConfig });
    expect(getFieldNameFromTrace).toHaveBeenCalledWith({ series: testConfig.series, fullPath: 'Name1' });

    expect(chartLayoutGenerators.getBarChartTraceOffsetSettings).toHaveBeenCalledWith('group', {
      yaxis: 'y1',
      totalAxis: 4,
      axisNumber: 1,
      traceIndex: 1,
      totalTraces: 4,
      effectiveTimerange: {
        from: '2024-08-11T14:56:10.000Z',
        to: '2024-08-12T15:01:10.000Z',
        type: 'absolute',
      },
      isTimeline: false,
      xAxisItemsLength: 10,
    });

    expect(barChartDataSettingsWithCustomUnits).toEqual({
      fullPath: 'Name1',
      offset: -0.375,
      offsetgroup: 1,
      width: 0.25,
      y: [1, 2, 3],
      yaxis: 'y1',
    });
  });
});
