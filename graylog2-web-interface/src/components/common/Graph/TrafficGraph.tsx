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
import React from 'react';
import styled, { css, useTheme } from 'styled-components';

import { Spinner } from 'components/common';
import type { PlotLayout } from 'views/components/visualizations/GenericPlot';
import GenericPlot from 'views/components/visualizations/GenericPlot';
import AppConfig from 'util/AppConfig';

type Props = {
  traffic: { [key: string]: number },
  width: number,
  trafficLimit?: number;
};

const GraphWrapper = styled.div<{
  $width: number,
}>(({
  $width,
}) => css`
  height: 200px;
  width: ${$width}px
`);

const TrafficGraph = ({ width, traffic, trafficLimit = undefined }: Props) => {
  const theme = useTheme();
  const isCloud = AppConfig.isCloud();

  const getMaxDailyValue = (arr) => arr.reduce((a, b) => Math.max(a, b));

  const range = getMaxDailyValue(Object.values(traffic));

  if (!traffic) {
    return <Spinner />;
  }

  const trafficLimitAnnotation: Partial<PlotLayout> = {
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
  };

  const trafficLimitAnnotationShape: Partial<PlotLayout> = {
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
  };

  const chartData = [{
    type: 'bar',
    x: Object.keys(traffic),
    y: Object.values(traffic),
  }];

  const layout: Partial<PlotLayout> = {
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
    yaxis: {
      range: isCloud ? [0, range] : null,
      title: {
        text: 'Bytes',
      },
      rangemode: 'tozero',
      hoverformat: '.4s',
      tickformat: 's',
    },
    updatemenus: [
      {
        buttons: [
          {
            args: ['yaxis.range', [0, isCloud ? trafficLimit : range]],
            args2: [{ 'yaxis.autorange': 'True' }, { 'yaxis.range': null }],
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
        visible: trafficLimit && (range < trafficLimit),
        xanchor: 'right',
        yanchor: 'top',
        x: 1,
        y: 1.3,
      },
    ],
  };

  const layoutWithTrafficLimit = { ...layout, ...trafficLimitAnnotation, ...trafficLimitAnnotationShape };

  const trafficLayout = trafficLimit ? layoutWithTrafficLimit : layout;

  return (
    <GraphWrapper $width={width}>
      <GenericPlot chartData={chartData}
                   layout={trafficLayout} />
    </GraphWrapper>
  );
};

export default TrafficGraph;
