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
import type { DefaultTheme } from 'styled-components';

import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import type { UnitTypeMapper } from 'views/components/visualizations/utils/chartLayoutGenerators';

export const layoutMapperWith4AxisFor4series = {
  fieldNameToAxisCountMapper: {
    field1: 1,
    field2: 2,
    field3: 3,
    no_field_name_series: 4,
  },
  fieldNameToAxisNameMapper: {
    field1: 'y',
    field2: 'y2',
    field3: 'y3',
    no_field_name_series: 'y4',
  },
  mapperAxisNumber: {
    'avg(field1)': 1,
    'avg(field2)': 2,
    'avg(field3)': 3,
    'count()': 4,
  },
  seriesUnitMapper: {
    'avg(field1)': 'time',
    'avg(field2)': 'size',
    'avg(field3)': 'percent',
    'count()': 'withoutUnit',
  },
  unitTypeMapper: {
    percent: {
      axisCount: 3,
      axisKeyName: 'yaxis3',
    },
    size: {
      axisCount: 2,
      axisKeyName: 'yaxis2',
    },
    time: {
      axisCount: 1,
      axisKeyName: 'yaxis',
    },
    withoutUnit: {
      axisCount: 4,
      axisKeyName: 'yaxis4',
    },
  },
  yAxisMapper: {
    'avg(field1)': 'y',
    'avg(field2)': 'y2',
    'avg(field3)': 'y3',
    'count()': 'y4',
  },
};

export const layoutMapperWith4AxisFor6series = {
  fieldNameToAxisCountMapper: {
    field1: 1,
    field2: 2,
    field3: 3,
    no_field_name_series: 4,
  },
  fieldNameToAxisNameMapper: {
    field1: 'y',
    field2: 'y2',
    field3: 'y3',
    no_field_name_series: 'y4',
  },
  mapperAxisNumber: {
    'avg(field1)': 1,
    'avg(field2)': 2,
    'avg(field3)': 3,
    'count()': 4,
    'latest(field3)': 3,
    'sum(field2)': 2,
  },
  seriesUnitMapper: {
    'avg(field1)': 'time',
    'avg(field2)': 'size',
    'avg(field3)': 'percent',
    'count()': 'withoutUnit',
    'latest(field3)': 'percent',
    'sum(field2)': 'size',
  },
  unitTypeMapper: {
    percent: {
      axisCount: 3,
      axisKeyName: 'yaxis3',
    },
    size: {
      axisCount: 2,
      axisKeyName: 'yaxis2',
    },
    time: {
      axisCount: 1,
      axisKeyName: 'yaxis',
    },
    withoutUnit: {
      axisCount: 4,
      axisKeyName: 'yaxis4',
    },
  },
  yAxisMapper: {
    'avg(field1)': 'y',
    'avg(field2)': 'y2',
    'avg(field3)': 'y3',
    'count()': 'y4',
    'latest(field3)': 'y3',
    'sum(field2)': 'y2',
  },
};

export const chartData4Charts: Array<ChartDefinition> = [
  {
    type: 'bar',
    name: 'Name1',
    fullPath: 'Name1',
    x: [
      '2024-08-11T16:00:00.000+02:00',
      '2024-08-11T18:00:00.000+02:00',
      '2024-08-11T20:00:00.000+02:00',
      '2024-08-11T22:00:00.000+02:00',
      '2024-08-12T00:00:00.000+02:00',
      '2024-08-12T02:00:00.000+02:00',
      '2024-08-12T04:00:00.000+02:00',
      '2024-08-12T06:00:00.000+02:00',
      '2024-08-12T08:00:00.000+02:00',
      '2024-08-12T10:00:00.000+02:00',
      '2024-08-12T12:00:00.000+02:00',
      '2024-08-12T14:00:00.000+02:00',
      '2024-08-12T16:00:00.000+02:00',
    ],
    y: [
      0.209526395173454,
      0.20606304909560721,
      0.2045749718151071,
      0.20425480283114256,
      0.20677467811158798,
      0.20645807770961144,
      0.20535445544554454,
      0.20544767899291896,
      0.20599992046975862,
      0.20602506644991617,
      0.20605278798408763,
      0.20609160421539727,
      0.20599850540664644,
    ],
    opacity: 1,
    originalName: 'Name1',
    yaxis: 'y',
    text: [
      '209.5 ms',
      '206.1 ms',
      '204.6 ms',
      '204.3 ms',
      '206.8 ms',
      '206.5 ms',
      '205.4 ms',
      '205.4 ms',
      '206.0 ms',
      '206.0 ms',
      '206.1 ms',
      '206.1 ms',
      '206.0 ms',
    ],
    hovertemplate: '%{text}<br><extra>%{meta}</extra>',
    meta: 'Name1',
    offsetgroup: 0,
    width: 1667307.6923076923,
    offset: -2500961.5384615385,
    marker: {
      color: '#4478b3',
      size: 1,
    },
  },
  {
    type: 'bar',
    name: 'Name2',
    fullPath: 'Name2',
    x: [
      '2024-08-11T16:00:00.000+02:00',
      '2024-08-11T18:00:00.000+02:00',
      '2024-08-11T20:00:00.000+02:00',
      '2024-08-11T22:00:00.000+02:00',
      '2024-08-12T00:00:00.000+02:00',
      '2024-08-12T02:00:00.000+02:00',
      '2024-08-12T04:00:00.000+02:00',
      '2024-08-12T06:00:00.000+02:00',
      '2024-08-12T08:00:00.000+02:00',
      '2024-08-12T10:00:00.000+02:00',
      '2024-08-12T12:00:00.000+02:00',
      '2024-08-12T14:00:00.000+02:00',
      '2024-08-12T16:00:00.000+02:00',
    ],
    y: [
      510,
      510,
      510,
      510,
      510,
      510,
      510,
      510,
      510,
      510,
      510,
      521,
      510,
    ],
    opacity: 1,
    originalName: 'Name2',
    yaxis: 'y2',
    text: [
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '510.0 b',
      '521.0 b',
      '510.0 b',
    ],
    hovertemplate: '%{text}<br><extra>%{meta}</extra>',
    meta: 'Name2',
    offsetgroup: 1,
    width: 1667307.6923076923,
    offset: -833653.8461538462,
    marker: {
      color: '#fd9e48',
      size: 1,
    },
  },
  {
    type: 'bar',
    name: 'Name3',
    fullPath: 'Name3',
    x: [
      '2024-08-11T16:00:00.000+02:00',
      '2024-08-11T18:00:00.000+02:00',
      '2024-08-11T20:00:00.000+02:00',
      '2024-08-11T22:00:00.000+02:00',
      '2024-08-12T00:00:00.000+02:00',
      '2024-08-12T02:00:00.000+02:00',
      '2024-08-12T04:00:00.000+02:00',
      '2024-08-12T06:00:00.000+02:00',
      '2024-08-12T08:00:00.000+02:00',
      '2024-08-12T10:00:00.000+02:00',
      '2024-08-12T12:00:00.000+02:00',
      '2024-08-12T14:00:00.000+02:00',
      '2024-08-12T16:00:00.000+02:00',
    ],
    y: [
      0.44,
      0.46,
      0.48,
      0.45,
      0.48,
      0.44,
      0.5,
      0.56,
      0.59,
      0.59,
      0.59,
      0.59,
      0.01,
    ],
    opacity: 1,
    originalName: 'Name3',
    yaxis: 'y3',
    offsetgroup: 2,
    width: 1667307.6923076923,
    offset: 833653.846153846,
    marker: {
      color: '#5d8947',
      size: 1,
    },
  },
  {
    type: 'bar',
    name: 'count()',
    fullPath: 'count()',
    x: [
      '2024-08-11T16:00:00.000+02:00',
      '2024-08-11T18:00:00.000+02:00',
      '2024-08-11T20:00:00.000+02:00',
      '2024-08-11T22:00:00.000+02:00',
      '2024-08-12T00:00:00.000+02:00',
      '2024-08-12T02:00:00.000+02:00',
      '2024-08-12T04:00:00.000+02:00',
      '2024-08-12T06:00:00.000+02:00',
      '2024-08-12T08:00:00.000+02:00',
      '2024-08-12T10:00:00.000+02:00',
      '2024-08-12T12:00:00.000+02:00',
      '2024-08-12T14:00:00.000+02:00',
      '2024-08-12T16:00:00.000+02:00',
    ],
    y: [
      663,
      1935,
      1774,
      989,
      1864,
      978,
      1515,
      1271,
      75443,
      214071,
      210654,
      197087,
      111067,
    ],
    opacity: 1,
    originalName: 'count()',
    yaxis: 'y4',
    offsetgroup: 3,
    width: 1667307.6923076923,
    offset: 2500961.5384615385,
    marker: {
      color: '#ffcdd2',
      size: 1,
    },
  },
];

export const unitTypeMapper4Charts: UnitTypeMapper = {
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
};

export const theme = {
  colors: {
    variant: { lightest: { default: '#000' } },
    global: { textDefault: '#fff' },
  },
  fonts: {
    family: { body: 'defaultFont' },
    size: { small: '1rem' },
  },
} as DefaultTheme;

export const layoutsFor4axis = {
  yaxis: {
    automargin: true,
    autoshift: true,
    fixedrange: true,
    gridcolor: '#000',
    position: 0,
    rangemode: 'tozero',
    side: 'left',
    tickfont: {
      color: '#fff',
      family: 'defaultFont',
      size: 16,
    },
    ticktext: [
      '52.4 ms',
      '104.8 ms',
      '157.1 ms',
      '209.5 ms',
    ],
    tickvals: [
      0.0523815987933635,
      0.104763197586727,
      0.1571447963800905,
      0.209526395173454,
    ],
    title: {
      font: {
        color: '#fff',
        family: 'defaultFont',
        size: 16,
      },
    },
  },
  yaxis2: {
    automargin: true,
    autoshift: true,
    fixedrange: true,
    gridcolor: '#000',
    overlaying: 'y',
    position: 1,
    rangemode: 'tozero',
    side: 'right',
    tickfont: {
      color: '#fff',
      family: 'defaultFont',
      size: 16,
    },
    ticktext: [
      '130.3 B',
      '260.5 B',
      '390.8 B',
      '521.0 B',
    ],
    tickvals: [
      130.25,
      260.5,
      390.75,
      521,
    ],
    title: {
      font: {
        color: '#fff',
        family: 'defaultFont',
        size: 16,
      },
    },
  },
  yaxis3: {
    automargin: true,
    autoshift: true,
    fixedrange: true,
    gridcolor: '#000',
    overlaying: 'y',
    position: 0.1,
    rangemode: 'tozero',
    side: 'left',
    tickfont: {
      color: '#fff',
      family: 'defaultFont',
      size: 16,
    },
    tickformat: '.1%',
    title: {
      font: {
        color: '#fff',
        family: 'defaultFont',
        size: 16,
      },
    },
  },
  yaxis4: {
    automargin: true,
    autoshift: true,
    fixedrange: true,
    gridcolor: '#000',
    overlaying: 'y',
    position: 0.9,
    rangemode: 'tozero',
    side: 'right',
    tickfont: {
      color: '#fff',
      family: 'defaultFont',
      size: 16,
    },
    tickformat: ',~r',
    ticklabelposition: 'inside',
    title: {
      font: {
        color: '#fff',
        family: 'defaultFont',
        size: 16,
      },
    },
  },
};
