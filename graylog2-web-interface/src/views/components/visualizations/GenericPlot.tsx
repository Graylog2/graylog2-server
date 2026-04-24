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
import * as React from 'react';
import { useContext, useMemo, useCallback, useRef } from 'react';
import merge from 'lodash/merge';
import { useTheme } from 'styled-components';

import type { Layout, PlotMouseEvent, EChartsInstance } from 'views/components/visualizations/types';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import { ROOT_FONT_SIZE } from 'theme/constants';
import getDefaultPlotYLayoutSettings from 'views/components/visualizations/utils/getDefaultPlotYLayoutSettings';
import EChart from 'views/components/visualizations/echarts/EChart';
import plotlyToECharts from 'views/components/visualizations/echarts/plotlyAdapter';

import ChartColorContext from './ChartColorContext';

import InteractiveContext from '../contexts/InteractiveContext';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

export type PlotLayout = Layout;

export type OnClickMarkerEvent = {
  x: string;
  y: string;
};

export type OnHoverMarkerEvent = {
  positionX: number;
  positionY: number;
  x: string;
  y: string;
};

type ChartMarker = {
  colors?: Array<string>;
  color?: string;
  size?: number;
};

export type ChartConfig = {
  name: string;
  labels: Array<string>;
  originalLabels?: Array<string>;
  line?: ChartMarker;
  marker?: ChartMarker;
  originalName?: string;
};

export type ChartColor = {
  line?: ChartMarker;
  marker?: ChartMarker;
  outsidetextfont?: {
    color: string;
  };
};

type Props = {
  chartData: Array<any>;
  layout?: Partial<PlotLayout>;
  onZoom?: (from: string, to: string) => void;
  setChartColor?: (data: ChartConfig, color: ColorMapper) => ChartColor;
  onClickMarker?: (markerEvent: OnClickMarkerEvent, event?: PlotMouseEvent) => void;
  onHoverMarker?: (event: OnHoverMarkerEvent) => void;
  onUnhoverMarker?: () => void;
  onAfterPlot?: () => void;
  onInitialized?: (figure: unknown, graphDiv: EChartsInstance) => void;
};

const style = { height: '100%', width: '100%' };

const usePlotLayout = (layout: Partial<Layout>) => {
  const theme = useTheme();
  const interactive = useContext(InteractiveContext);
  const { colors } = useContext(ChartColorContext);

  return useMemo(() => {
    const fontSettings = {
      color: theme.colors.text.primary,
      size: ROOT_FONT_SIZE * Number(theme.fonts.size.small.replace(/rem|em/i, '')),
      family: theme.fonts.family.body,
    };

    const defaultLayout: Partial<Layout> = {
      shapes: [],
      autosize: true,
      showlegend: false,
      margin: {
        t: 10,
        l: 40,
        r: 10,
        b: 0,
        pad: 0,
      },
      legend: {
        orientation: 'h' as const,
        font: fontSettings,
      },
      hoverlabel: {
        namelength: -1,
      },
      paper_bgcolor: 'transparent',
      plot_bgcolor: 'transparent',
      title: {
        font: fontSettings,
      },
      yaxis: getDefaultPlotYLayoutSettings(theme),
      xaxis: {
        automargin: true,
        tickfont: fontSettings,
        title: {
          font: fontSettings,
        },
      },
    };

    const plotLayout = merge({}, defaultLayout, layout);

    plotLayout.shapes = (plotLayout.shapes ?? []).map((shape) => ({
      ...shape,
      line: { ...(shape?.line ?? {}), color: shape?.line?.color || colors.get(eventsDisplayName, EVENT_COLOR) },
    }));

    if (!interactive) {
      plotLayout.hovermode = false;
    }

    return plotLayout;
  }, [colors, interactive, layout, theme]);
};

const usePlotChartData = (
  chartData: Array<any>,
  setChartColor: (data: ChartConfig, color: ColorMapper) => ChartColor,
) => {
  const theme = useTheme();
  const { colors } = useContext(ChartColorContext);

  return useMemo(
    () =>
      chartData.map((chart) => {
        if (setChartColor && colors) {
          const conf = setChartColor(chart, colors);

          conf.outsidetextfont = { color: theme.colors.text.primary };

          if (chart?.name === eventsDisplayName) {
            const eventColor = colors.get(eventsDisplayName, EVENT_COLOR);

            conf.marker = { color: eventColor, size: 5 };
          }

          if (conf.line || conf.marker) {
            return merge(chart, conf);
          }

          return chart;
        }

        return chart;
      }),
    [chartData, colors, setChartColor, theme.colors.text.primary],
  );
};

const GenericPlot = ({
  chartData,
  layout = {},
  setChartColor = undefined,
  onClickMarker = () => {},
  onHoverMarker: _onHoverMarker = () => {},
  onUnhoverMarker: _onUnhoverMarker = () => {},
  onZoom = () => {},
  onAfterPlot = () => {},
  onInitialized = () => {},
}: Props) => {
  const interactive = useContext(InteractiveContext);
  const plotLayout = usePlotLayout(layout);
  const plotChartData = usePlotChartData(chartData, setChartColor);
  const onRenderComplete = useContext(RenderCompletionCallback);
  const chartInstanceRef = useRef<EChartsInstance | null>(null);

  const theme = useTheme();

  const option = useMemo(() => {
    const echartsOption = plotlyToECharts(plotChartData, plotLayout);

    echartsOption.tooltip = {
      ...echartsOption.tooltip,
      backgroundColor: theme.colors.global.contentBackground,
      borderColor: theme.colors.variant.light.default,
      textStyle: {
        color: theme.colors.text.primary,
        fontFamily: theme.fonts.family.body,
      },
    };

    return echartsOption;
  }, [plotChartData, plotLayout, theme]);

  const onChartReady = useCallback(
    (instance: EChartsInstance) => {
      chartInstanceRef.current = instance;
      onRenderComplete();
      onAfterPlot();
      onInitialized(null, instance);
    },
    [onRenderComplete, onAfterPlot, onInitialized],
  );

  const events = useMemo(() => {
    if (!interactive) return {};

    return {
      click: (params: any) => {
        const x = params.name ?? params.data?.[0] ?? '';
        const y = params.value ?? params.data?.[1] ?? '';

        const plotMouseEvent: PlotMouseEvent = {
          points: [
            {
              curveNumber: params.seriesIndex ?? 0,
              pointIndex: params.dataIndex,
              pointNumber: params.dataIndex,
              x,
              y,
              data: plotChartData[params.seriesIndex] ?? {},
              fullData: plotChartData[params.seriesIndex] ?? {},
              value: params.value,
              label: params.name,
              percent: params.percent,
              z: params.data?.[2],
              bbox: { x0: params.event?.offsetX ?? 0, y0: params.event?.offsetY ?? 0 },
            },
          ],
          event: params.event?.event ?? params.event,
        };

        onClickMarker({ x: String(x), y: String(y) }, plotMouseEvent);
      },
      datazoom: (params: any) => {
        if (params.batch?.[0]) {
          const { startValue, endValue } = params.batch[0];

          if (startValue && endValue) {
            onZoom(String(startValue), String(endValue));
          }
        }
      },
    };
  }, [interactive, onClickMarker, onZoom, plotChartData]);

  return (
    <EChart
      option={option}
      style={style}
      onEvents={events}
      onChartReady={onChartReady}
    />
  );
};

export default GenericPlot;
