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

import React, { useCallback } from 'react';
import PropTypes from 'prop-types';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import PlotLegend from 'views/components/visualizations/PlotLegend';
import useUserDateTime from 'hooks/useUserDateTime';
import type { AxisType } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { axisTypes, DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import assertUnreachable from 'logic/assertUnreachable';
import useAppDispatch from 'stores/useAppDispatch';

import GenericPlot from './GenericPlot';
import type { ChartColor, ChartConfig } from './GenericPlot';
import OnZoom from './OnZoom';

import CustomPropTypes from '../CustomPropTypes';

export type Props = {
  axisType?: AxisType,
  config: AggregationWidgetConfig,
  chartData: any,
  effectiveTimerange?: {
    from: string,
    to: string,
  },
  getChartColor?: (data: Array<ChartConfig>, name: string) => (string | undefined | null),
  height?: number;
  setChartColor?: (config: ChartConfig, color: ColorMapper) => ChartColor,
  plotLayout?: any,
  onZoom?: (from: string, to: string, userTimezone: string) => boolean,
};

const yLegendPosition = (containerHeight: number) => {
  if (containerHeight < 150) {
    return -0.6;
  }

  if (containerHeight < 400) {
    return -0.2;
  }

  return -0.14;
};

type Layout = {
  yaxis: { fixedrange?: boolean },
  legend?: { y?: number },
  showlegend: boolean,
  hovermode: 'x',
};

const mapAxisType = (axisType: AxisType): 'linear' | 'log' => {
  switch (axisType) {
    case 'linear': return 'linear';
    case 'logarithmic': return 'log';
    default: return assertUnreachable(axisType, 'Unable to parse axis type: ');
  }
};

const defaultSetColor = (chart: ChartConfig, colors: ColorMapper) => ({ line: { color: colors.get(chart.originalName ?? chart.name) } });

const XYPlot = ({
  axisType,
  config,
  chartData,
  effectiveTimerange,
  getChartColor,
  setChartColor,
  height,
  plotLayout = {},
  onZoom,
}: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const yaxis = { fixedrange: true, rangemode: 'tozero', tickformat: ',~r', type: mapAxisType(axisType) };
  const defaultLayout: Layout = {
    yaxis,
    showlegend: false,
    hovermode: 'x',
  };

  if (height) {
    defaultLayout.legend = { y: yLegendPosition(height) };
  }

  const layout = { ...defaultLayout, ...plotLayout };
  const dispatch = useAppDispatch();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const _onZoom = useCallback(config.isTimeline
    ? (from: string, to: string) => (onZoom ? onZoom(from, to, userTimezone) : dispatch(OnZoom(from, to, userTimezone)))
    : () => true, [config.isTimeline, onZoom]);

  if (config.isTimeline && effectiveTimerange) {
    const normalizedFrom = formatTime(effectiveTimerange.from, 'internal');
    const normalizedTo = formatTime(effectiveTimerange.to, 'internal');

    layout.xaxis = {
      range: [normalizedFrom, normalizedTo],
      type: 'date',
    };
  } else {
    layout.xaxis = {
      fixedrange: true,
      /* disable plotly sorting by setting the type of the xaxis to category */
      type: config.sort.length > 0 ? 'category' : undefined,
    };
  }

  return (
    <PlotLegend config={config} chartData={chartData}>
      <GenericPlot chartData={chartData}
                   layout={layout}
                   onZoom={_onZoom}
                   getChartColor={getChartColor}
                   setChartColor={setChartColor} />
    </PlotLegend>
  );
};

XYPlot.propTypes = {
  axisType: PropTypes.oneOf(axisTypes),
  chartData: PropTypes.array.isRequired,
  config: CustomPropTypes.instanceOf(AggregationWidgetConfig).isRequired,
  effectiveTimerange: PropTypes.exact({

    type: PropTypes.string.isRequired,
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
  }),
  plotLayout: PropTypes.object,
  getChartColor: PropTypes.func,
  setChartColor: PropTypes.func,
  onZoom: PropTypes.func,
};

XYPlot.defaultProps = {
  axisType: DEFAULT_AXIS_TYPE,
  plotLayout: {},
  getChartColor: undefined,
  setChartColor: defaultSetColor,
  effectiveTimerange: undefined,
  onZoom: undefined,
  height: undefined,
};

export default XYPlot;
