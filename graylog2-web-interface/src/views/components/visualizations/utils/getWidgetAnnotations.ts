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

import type { Shape, Annotations } from 'plotly.js';
import type { DefaultTheme } from 'styled-components';
import flatten from 'lodash/flatten';
import flatMap from 'lodash/flatMap';
import groupBy from 'lodash/groupBy';
import mapValues from 'lodash/mapValues';
import moment from 'moment';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { MappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import { NO_FIELD_NAME_SERIES } from 'views/components/visualizations/Constants';
import type { SeriesAnnotation } from 'views/logic/aggregationbuilder/SeriesConfig';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';

type AnnotationSettings = {
  shapes: Array<Partial<Shape>>;
  annotations: Array<Partial<Annotations>>;
};

const getFlattenXValues = (chartData: Array<ChartDefinition>) => flatten(chartData.map(({ x }) => x));
const groupYValuesByYaxis = (chartData: Array<ChartDefinition>) =>
  mapValues(groupBy(chartData, 'yaxis'), (group) => flatMap(group, ({ y }) => y ?? []));
const getTimelineRange = (xValues: ChartDefinition['x']) => {
  const moments = xValues.map((d) => moment(d));
  const from = moment.min(moments);
  const to = moment.max(moments);
  console.log({ from, to });

  return [from, to];
};
const getXLeftBoundary = (
  xValues: ChartDefinition['x'],
  isTimeline: boolean,
  formatTime: (time: DateTime, format?: DateTimeFormats) => string,
): string | number => {
  if (isTimeline) return formatTime(getTimelineRange(xValues)[0].toISOString(), 'internal');

  return -0.5;
};
const isXValueOnChart = (x: string, xValues: ChartDefinition['x'], isTimeline: boolean) => {
  if (!isTimeline) return !!xValues.find((xValue) => String(xValue) === x);

  const [from, to] = getTimelineRange(xValues);

  return moment(x).isBetween(from, to);
};
const isYValuesOnChart = (y: string, yValues: ChartDefinition['y']) => {
  const max = Math.max(...(yValues as Array<number>));
  const min = Math.min(...(yValues as Array<number>));

  return Number(y) >= min && Number(y) <= max;
};
const mockedTimeAnnotation = {
  showReferenceLines: true,
  y: '50',
  x: '2025-09-15T17:00:40.000Z',
  color: '#444555',
  note: 'sddfds sdlfkds s sdfsd sdf sdflsdkjf sf',
};

const mockedSimplAnnotation = {
  showReferenceLines: true,
  y: '1500',
  x: 'POST',
  color: '#444555',
  note: 'sddfds sdlfkds s sdfsd sdf sdflsdkjf sf',
};

const getWidgetAnnotations = (
  config: AggregationWidgetConfig,
  fieldNameToAxisNameMapper: MappersForYAxis['fieldNameToAxisNameMapper'],
  theme: DefaultTheme,
  chartData: Array<ChartDefinition>,
  formatTime: (time: DateTime, format?: DateTimeFormats) => string,
) => {
  const xValues = getFlattenXValues(chartData);
  const yValuesByAxis = groupYValuesByYaxis(chartData);
  console.log({ xValues, yValuesByAxis });
  const xLeftBoundary = getXLeftBoundary(xValues, config.isTimeline, formatTime);

  const seriesAnnotations: Array<Array<AnnotationSettings>> = config.series.map((curSeries) => {
    const { field } = parseSeries(curSeries.function) ?? {};
    const yref: Shape['yref'] = fieldNameToAxisNameMapper?.[field ?? NO_FIELD_NAME_SERIES] ?? 'y';
    const annotations: Array<SeriesAnnotation> = curSeries.config?.annotations ?? [
      mockedTimeAnnotation,
      mockedSimplAnnotation,
    ];

    return annotations.map(({ x, showReferenceLines, y, color, note }) => {
      const formattedXCoordinate = config?.isTimeline ? formatTime(x, 'internal') : x;

      const isAnnotationOnChart =
        isXValueOnChart(x, xValues, config.isTimeline) && isYValuesOnChart(y, yValuesByAxis[yref]);

      console.log(
        {
          x,
          showReferenceLines,
          y,
          color,
          note,
        },
        [isXValueOnChart(x, xValues, config.isTimeline), isYValuesOnChart(y, yValuesByAxis[yref])],
      );
      if (!isAnnotationOnChart) return { annotations: [], shapes: [] };

      return {
        annotations: [
          {
            x: formattedXCoordinate,
            y,
            // bgcolor: 'rgba(0,0,0,0)', // hide label box
            borderwidth: 0,
            xanchor: 'center',
            yanchor: 'middle',
            text: '     ',
            // text: note,
            bgcolor: color,
            font: { size: 4 },
            xref: 'x',

            yref,
            ax: 0,
            ay: -7,
            captureevents: true,
            showarrow: true,
            arrowhead: 4,
            arrowcolor: color, // ← color of the square head + shaft
            hovertext: note, // 'Some other text<br>Some text', // ← shows a popover on hover
            hoverlabel: { bgcolor: color, font: { color: theme.utils.contrastingColor(color) } },
          },
        ],
        shapes: showReferenceLines
          ? [
              {
                type: 'line',
                x0: formattedXCoordinate,
                x1: formattedXCoordinate,
                // y0: 0,
                y1: y,
                xref: 'x',
                yref,
                layer: 'above',
                line: { width: 2, dash: 'dot', color },
              },
              {
                type: 'line',
                x0: xLeftBoundary, // normalizedFrom,
                x1: formattedXCoordinate,
                y0: y,
                y1: y,
                xref: 'x',
                yref,
                layer: 'above',
                line: { width: 2, dash: 'dot', color },
              },
            ]
          : [],
      };
    });
  });

  const flattenSeriesAnnotations = flatten(seriesAnnotations);

  return {
    referenceLineShapes: flatMap(flattenSeriesAnnotations, (o) => o.shapes),
    widgetAnnotations: flatMap(flattenSeriesAnnotations, (o) => o.annotations),
  };
  /*
  const widgetAnnotations = annotations.map(({ x, y, showHelperLines, color, enableValueX, enableValueY }) => ({
    x,
    y,
    text: 'Label',
    xref: 'paper',
    yref: 'paper',
    ax: 0,
    ay: -20,
    font: { size: 12, color: 'orange' },
    captureevents: true,
    showarrow: true,
    arrowhead: 4,
    arrowcolor: 'orange', // ← color of the square head + shaft
    hovertext: 'Some other text<br>Some text', // ← shows a popover on hover
    hoverlabel: { bgcolor: '#222', font: { color: '#fff' } },
  }));

  return {
    widgetAnnotations,
    referenceLines: [
      {
        type: 'line',
        x0: x,
        x1: x,
        y0: 0, // adjust to your actual axis min if not 0
        y1: y,
        xref: 'x',
        yref: 'y',
        layer: 'above',
        line: { width: 2, dash: 'dot', color: 'orange' },
      },
      // horizontal dashed line to y-axis
      {
        type: 'line',
        x0: 0, // or use current x-axis min dynamically
        x1: xA,
        y0: yA,
        y1: yA,
        xref: 'x',
        yref: 'y',
        layer: 'above',
        line: { width: 2, dash: 'dot', color: 'orange' },
      },
    ],
  };

   */
};

export default getWidgetAnnotations;
