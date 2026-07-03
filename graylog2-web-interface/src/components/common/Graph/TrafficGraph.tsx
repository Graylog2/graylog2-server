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
import {
  getHoverTemplateSettings,
  getFormatSettingsByData,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';

type Props = {
  traffic: { [key: string]: number };
  width: number;
  trafficLimit?: number;
  zoomedToData?: boolean;
  uiRevision?: number;
  onUserZoom?: () => void;
  onUserZoomReset?: () => void;
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

const PLOT_CONFIG = { doubleClick: 'reset' as const };

const TrafficGraph = ({
  width,
  traffic,
  trafficLimit = undefined,
  zoomedToData = false,
  uiRevision = 1,
  onUserZoom = undefined,
  onUserZoomReset = undefined,
}: Props) => {
  const theme = useTheme();

  const yValues = useMemo(() => Object.values(traffic), [traffic]);

  const chartData = useMemo(
    () => [
      {
        type: 'bar',
        x: Object.keys(traffic),
        y: yValues,
        outsidetextfont: { color: theme.colors.text.primary },
        ...getHoverTemplateSettings({
          convertedValues: yValues,
          unit: FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'binary_size' }),
        }),
      },
    ],
    [theme.colors.text.primary, traffic, yValues],
  );

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
      ...(getFormatSettingsByData('binary_size', valuesToGetFormatSettings) as GeneratedLayout),
    }),
    [valuesToGetFormatSettings],
  );
  const zoomedLayout = useMemo(
    () => ({
      rangemode: 'tozero',
      ...(getFormatSettingsByData('binary_size', yValues) as GeneratedLayout),
    }),
    [yValues],
  );

  const layout: Partial<PlotLayout> = useMemo(
    () => ({
      showlegend: false,
      margin: {
        l: 60,
        t: 28,
      },
      xaxis: {
        type: 'date',
        tickformat: '%b %d',
        hoverformat: '%b %d, %Y',
        title: {
          text: 'Date (UTC)',
        },
      },
      hovermode: 'x',
      hoverlabel: {
        namelength: -1,
      },
      yaxis: { ...(zoomedToData ? zoomedLayout : notZoomedLayout), fixedrange: true },
      uirevision: uiRevision,
    }),
    [notZoomedLayout, uiRevision, zoomedLayout, zoomedToData],
  );

  const trafficLayout = useMemo(() => {
    const layoutWithTrafficLimit = { ...layout, ...trafficLimitAnnotation, ...trafficLimitAnnotationShape };

    return trafficLimit ? layoutWithTrafficLimit : layout;
  }, [layout, trafficLimit, trafficLimitAnnotation, trafficLimitAnnotationShape]);

  return (
    <GraphWrapper $width={width}>
      <GenericPlot
        chartData={chartData}
        layout={trafficLayout}
        config={PLOT_CONFIG}
        onZoom={onUserZoom}
        onZoomReset={onUserZoomReset}
      />
    </GraphWrapper>
  );
};

export default TrafficGraph;
