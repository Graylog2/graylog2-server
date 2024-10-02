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

jest.mock('hooks/useFeature');
jest.mock('views/components/visualizations/hooks/useWidgetUnits');

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

describe('useChartDataSettingsWithCustomUnits', () => {
  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);
    asMock(useWidgetUnits).mockReturnValue(units);
  });

  it('for time: returns beautiful values in text, converted to base unit values and correct yaxis', async () => {
    const { result } = renderHook(() => useChartDataSettingsWithCustomUnits(
      { config: testConfig },
    ));

    let chartDataSettingsWithCustomUnits;

    act(() => {
      chartDataSettingsWithCustomUnits = result.current(
        { name: 'Name1', fullPath: 'Name1', values: [1000, 2000, 3000] },
      );
    });

    expect(chartDataSettingsWithCustomUnits).toEqual({
      fullPath: 'Name1',
      hovertemplate: '%{text}<br><extra>%{meta}</extra>',
      meta: 'Name1',
      text: ['1.0 s', '2.0 s', '3.0 s'],
      y: [1, 2, 3],
      yaxis: 'y',
    });
  });

  it('for size: returns beautiful values in text, converted to base unit values and correct yaxis', async () => {
    const { result } = renderHook(() => useChartDataSettingsWithCustomUnits(
      { config: testConfig },
    ));

    let chartDataSettingsWithCustomUnits;

    act(() => {
      chartDataSettingsWithCustomUnits = result.current(
        { name: 'Name2', fullPath: 'Name2', values: [1000, 2000, 3000] },
      );
    });

    expect(chartDataSettingsWithCustomUnits).toEqual({
      fullPath: 'Name2',
      hovertemplate: '%{text}<br><extra>%{meta}</extra>',
      meta: 'Name2',
      text: ['1.0 MB', '2.0 MB', '3.0 MB'],
      y: [1000000, 2000000, 3000000],
      yaxis: 'y2',
    });
  });

  it('for percent: returns converted to base unit values and correct yaxis', async () => {
    const { result } = renderHook(() => useChartDataSettingsWithCustomUnits(
      { config: testConfig },
    ));

    let chartDataSettingsWithCustomUnits;

    act(() => {
      chartDataSettingsWithCustomUnits = result.current(
        { name: 'Name3', fullPath: 'Name3', values: [100, 200, 300] },
      );
    });

    expect(chartDataSettingsWithCustomUnits).toEqual({
      fullPath: 'Name3',
      y: [1, 2, 3],
      yaxis: 'y3',
    });
  });

  it('for non unit: returns correct yaxis and data as it is', async () => {
    const { result } = renderHook(() => useChartDataSettingsWithCustomUnits(
      { config: testConfig },
    ));

    let chartDataSettingsWithCustomUnits;

    act(() => {
      chartDataSettingsWithCustomUnits = result.current(
        { name: 'count()', fullPath: 'count()', values: [100, 200, 300] },
      );
    });

    expect(chartDataSettingsWithCustomUnits).toEqual({
      fullPath: 'count()',
      y: [100, 200, 300],
      yaxis: 'y4',
    });
  });
});
