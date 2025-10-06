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
import React, { useMemo } from 'react';
import styled, { css, useTheme } from 'styled-components';

import type { PlotLayout } from 'views/components/visualizations/GenericPlot';
import GenericPlot from 'views/components/visualizations/GenericPlot';
import AppConfig from 'util/AppConfig';
import {
  getHoverTemplateSettings,
  getFormatSettingsByData,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';

type Props = {
  traffic: { [key: string]: number };
  width: number;
  trafficLimit?: number;
};

const GraphWrapper = styled.div<{
  $width: number;
}>(
  ({ $width }) => css`
    height: 200px;
    width: ${$width}px;
  `,
);

type GeneratedLayout = {
  range: [number, number];
  tickvals: Array<number>;
  ticktext: Array<string>;
};
const TrafficGraph = ({ width, traffic, trafficLimit = undefined }: Props) => {
  const theme = useTheme();
  const isCloud = AppConfig.isCloud();

  const getMaxDailyValue = (arr: Array<number>) => arr.reduce((a, b) => Math.max(a, b));

  const yValues = useMemo(() => Object.values(traffic), [traffic]);

  const chartData = useMemo(
    () => [
      {
        type: 'bar',
        x: Object.keys(traffic),
        y: yValues,
        ...getHoverTemplateSettings({
          convertedValues: yValues,
          unit: FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'size' }),
        }),
      },
    ],
    [traffic, yValues],
  );

  const maxDailyValue = useMemo(() => getMaxDailyValue(yValues), [yValues]);

  const trafficLimitAnnotation: Partial<PlotLayout> = useMemo(
    () => ({
      annotations: [
        {
          showarrow: false,
          text: '<b>Licensed traffic limit</b>',
          align: 'right',
          x: 1,
          xref: 'paper',
          xanchor: 'right',
          y: trafficLimit,
          yanchor: 'bottom',
          font: {
            color: theme.colors.variant.danger,
          },
        },
      ],
    }),
    [theme.colors.variant.danger, trafficLimit],
  );

  const trafficLimitAnnotationShape: Partial<PlotLayout> = useMemo(
    () => ({
      shapes: [
        {
          type: 'line',
          x0: 0,
          x1: 1,
          y0: trafficLimit,
          y1: trafficLimit,
          name: 'Traffic Limit',
          xref: 'paper',
          yref: 'y',
          line: {
            color: theme.colors.variant.danger,
          },
        },
      ],
    }),
    [theme.colors.variant.danger, trafficLimit],
  );

  const valuesToGetFormatSettings = useMemo(
    () => (trafficLimit ? [...yValues, trafficLimit] : yValues),
    [trafficLimit, yValues],
  );

  const notZoomedLayout = useMemo<GeneratedLayout>(
    () => ({
      rangemode: 'tozero',
      ...(getFormatSettingsByData('size', valuesToGetFormatSettings) as GeneratedLayout),
    }),
    [valuesToGetFormatSettings],
  );
  const zoomedLayout = useMemo(
    () => ({
      rangemode: 'tozero',
      ...(getFormatSettingsByData('size', yValues) as GeneratedLayout),
    }),
    [yValues],
  );

  const layout: Partial<PlotLayout> = useMemo(
    () => ({
      showlegend: false,
      margin: {
        l: 60,
      },
      xaxis: {
        type: 'date',
        title: {
          text: 'Time',
        },
      },
      hovermode: 'x',
      hoverlabel: {
        namelength: -1,
      },
      yaxis: notZoomedLayout,
      updatemenus: [
        {
          buttons: [
            {
              // args: ['yaxis.range', [0, isCloud ? trafficLimit : range]],
              args: [
                {
                  'yaxis.range': zoomedLayout.range,
                  'yaxis.tickvals': zoomedLayout.tickvals,
                  'yaxis.ticktext': zoomedLayout.ticktext,
                },
              ],
              args2: [
                {
                  'yaxis.range': notZoomedLayout.range,
                  'yaxis.tickvals': notZoomedLayout.tickvals,
                  'yaxis.ticktext': notZoomedLayout.ticktext,
                },
              ],
              label: 'Zoom/Reset',
              method: 'relayout',
            },
          ],
          direction: 'right',
          showactive: false,
          bordercolor: theme.colors.global.contentBackground,
          font: {
            color: theme.colors.global.link,
          },
          active: 1,
          type: 'buttons',
          visible: trafficLimit && maxDailyValue < trafficLimit && !isCloud,
          xanchor: 'right',
          yanchor: 'top',
          x: 1,
          y: 1.3,
        },
      ],
    }),
    [
      isCloud,
      notZoomedLayout,
      maxDailyValue,
      theme.colors.global.contentBackground,
      theme.colors.global.link,
      trafficLimit,
      zoomedLayout.range,
      zoomedLayout.ticktext,
      zoomedLayout.tickvals,
    ],
  );

  const trafficLayout = useMemo(() => {
    const layoutWithTrafficLimit = { ...layout, ...trafficLimitAnnotation, ...trafficLimitAnnotationShape };

    return trafficLimit ? layoutWithTrafficLimit : layout;
  }, [layout, trafficLimit, trafficLimitAnnotation, trafficLimitAnnotationShape]);

  return (
    <GraphWrapper $width={width}>
      <GenericPlot chartData={chartData} layout={trafficLayout} />
    </GraphWrapper>
  );
};

export default TrafficGraph;
