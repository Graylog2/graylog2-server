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
import type { AdditionalSettings, GenerateLayoutsParams } from 'views/components/visualizations/utils/chartLayoutGenerators';
import {
  getBarChartTraceOffsetSettings,
  generateMappersForYAxis,
  generateLayouts,
  getHoverTemplateSettings,
  getPieHoverTemplateSettings,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import Series from 'views/logic/aggregationbuilder/Series';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import {
  layoutMapperWith4AxisFor6series,
  layoutMapperWith4AxisFor4series,
  chartData4Charts,
  theme,
  unitTypeMapper4Charts,
  layoutsFor4axis,
} from 'views/components/visualizations/utils/__tests__/fixtures';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

describe('Chart Layout Generators', () => {
  describe('getBarChartTraceOffsetSettings', () => {
    const defaultProps: AdditionalSettings = {
      yaxis: 'y3',
      totalAxis: 4,
      axisNumber: 1,
      traceIndex: 2,
      totalTraces: 4,
      effectiveTimerange: {
        from: '2024-08-11T14:56:10.000Z',
        to: '2024-08-12T15:01:10.000Z',
        type: 'absolute',
      },
      isTimeline: false,
      xAxisItemsLength: 10,
    };

    it('return correct offset for barmode, mode group, non timeline', () => {
      const result = getBarChartTraceOffsetSettings('group', defaultProps);

      expect(result).toEqual({
        offsetgroup: 2,
        width: 0.25,
        offset: 0.125,
      });
    });

    it('return correct offset for barmode, mode group, timeline', () => {
      const result = getBarChartTraceOffsetSettings('group', {
        ...defaultProps,
        isTimeline: true,
      });

      expect(result).toEqual({
        offsetgroup: 2,
        width: 2167500,
        offset: 1083750,
      });
    });

    it('return correct offset for barmode, mode stack, non timeline', () => {
      const result = getBarChartTraceOffsetSettings('stack', {
        ...defaultProps,
        yaxis: 'y2',
        totalAxis: 2,
        axisNumber: 2,
        traceIndex: 1,
        totalTraces: 2,
      });

      expect(result).toEqual({
        offsetgroup: 'y2',
        width: 0.5,
        offset: 0.25,
      });
    });

    it('return correct offset for barmode, mode stack, timeline', () => {
      const result = getBarChartTraceOffsetSettings('stack', {
        ...defaultProps,
        isTimeline: true,
        yaxis: 'y2',
        totalAxis: 2,
        axisNumber: 2,
        traceIndex: 1,
        totalTraces: 2,
      });

      expect(result).toEqual({
        offsetgroup: 'y2',
        width: 4335000,
        offset: 2167500,
      });
    });
  });

  describe('generateMappersForYAxis', () => {
    const series = [
      Series.create('avg', 'field1'),
      Series.create('avg', 'field2'),
      Series.create('avg', 'field3'),
      Series.create('count'),
    ];
    const units = UnitsConfig
      .empty().toBuilder()
      .setFieldUnit('field1', new FieldUnit('time', 'ms'))
      .setFieldUnit('field2', new FieldUnit('size', 'b'))
      .setFieldUnit('field3', new FieldUnit('percent', '%'))
      .build();

    it('creates mappers for 4 different axis when each field has different unit', () => {
      const result = generateMappersForYAxis({ series, units });

      expect(result).toEqual(layoutMapperWith4AxisFor4series);
    });

    it('creates mappers for 4 different axis when some fields has same unit', () => {
      const series2 = [
        ...series,
        Series.create('sum', 'field2'),
        Series.create('latest', 'field3'),
      ];
      const result = generateMappersForYAxis({ series: series2, units });

      expect(result).toEqual(layoutMapperWith4AxisFor6series);
    });
  });

  describe('generateLayouts', () => {
    const configForLayout: AggregationWidgetConfig = AggregationWidgetConfig.builder().series([
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
      .setFieldUnit('fieldSize', new FieldUnit('size', 'b'))
      .setFieldUnit('fieldPercent', new FieldUnit('percent', '%'))
      .build();

    const params = {
      config: configForLayout, chartData: chartData4Charts, theme: theme, barmode: 'group', unitTypeMapper: unitTypeMapper4Charts, widgetUnits: units,
    } as GenerateLayoutsParams;

    it('for 4 different axis including the one with tickvals and ticktexts', () => {
      const result = generateLayouts(params);

      expect(result).toEqual(layoutsFor4axis);
    });

    it('does not throw exception when chart data is `undefined` in stack mode', () => {
      const result = generateLayouts({ ...params, chartData: [], barmode: 'stack' });

      expect(result).toBeDefined();
    });
  });

  describe('getHoverTemplateSettings', () => {
    it('for time', () => {
      const result = getHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' }),
        name: 'Name1',
      });

      expect(result).toEqual({
        hovertemplate: '%{text}<br><extra>%{meta}</extra>',
        meta: 'Name1',
        text: [
          '10.0 ms',
          '20.0 ms',
          '30.0 ms',
        ],
      });
    });

    it('for size', () => {
      const result = getHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'size' }),
        name: 'Name2',
      });

      expect(result).toEqual({
        hovertemplate: '%{text}<br><extra>%{meta}</extra>',
        meta: 'Name2',
        text: [
          '10.0 B',
          '20.0 B',
          '30.0 B',
        ],
      });
    });

    it('for percentage', () => {
      const result = getHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: '%', unit_type: 'percent' }),
        name: 'Name3',
      });

      expect(result).toEqual({});
    });

    it('for without unit', () => {
      const result = getHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: null,
        name: 'Name4',
      });

      expect(result).toEqual({});
    });
  });

  describe('getPieHoverTemplateSettings', () => {
    it('for time', () => {
      const result = getPieHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' }),
        name: 'Name1',
      });

      expect(result).toEqual({
        hovertemplate: '<b>%{label}</b><br>%{text}<br>%{percent}',
        textinfo: 'percent',
        meta: 'Name1',
        text: [
          '10.0 ms',
          '20.0 ms',
          '30.0 ms',
        ],
      });
    });

    it('for size', () => {
      const result = getPieHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'size' }),
        name: 'Name2',
      });

      expect(result).toEqual({
        hovertemplate: '<b>%{label}</b><br>%{text}<br>%{percent}',
        textinfo: 'percent',
        meta: 'Name2',
        text: [
          '10.0 B',
          '20.0 B',
          '30.0 B',
        ],
      });
    });

    it('for percentage', () => {
      const result = getPieHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: FieldUnit.fromJSON({ abbrev: '%', unit_type: 'percent' }),
        name: 'Name3',
      });

      expect(result).toEqual({
        hovertemplate: '<b>%{label}</b><br>%{text}<br>%{percent}',
        textinfo: 'percent',
        meta: 'Name3',
        text: [
          '10.0 %',
          '20.0 %',
          '30.0 %',
        ],
      });
    });

    it('for without unit', () => {
      const result = getPieHoverTemplateSettings({
        convertedValues: [10, 20, 30],
        unit: null,
        name: 'Name4',
      });

      expect(result).toEqual({
        hovertemplate: '<b>%{label}</b><br>%{text}<br>%{percent}',
        textinfo: 'percent',
        meta: 'Name4',
        text: [
          10,
          20,
          30,
        ],
      });
    });
  });
});
