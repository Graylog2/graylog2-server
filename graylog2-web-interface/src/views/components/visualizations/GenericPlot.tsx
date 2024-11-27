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
import { useContext, useMemo, useCallback } from 'react';
import styled, { css, useTheme } from 'styled-components';
import merge from 'lodash/merge';
import type { Layout } from 'plotly.js';

import Plot from 'views/components/visualizations/plotly/AsyncPlot';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import { ROOT_FONT_SIZE } from 'theme/constants';
import getDefaultPlotYLayoutSettings from 'views/components/visualizations/utils/getDefaultPlotYLayoutSettings';

import ChartColorContext from './ChartColorContext';

import InteractiveContext from '../contexts/InteractiveContext';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

export type PlotLayout = Layout

const StyledPlot = styled(Plot)(({ theme }) => css`
  div.plotly-notifier {
    visibility: hidden;
  }

  .customPopover .popover-content {
    padding: 0;
  }
  
  .hoverlayer .hovertext {
    rect {
      fill: ${theme.colors.global.contentBackground} !important;
      opacity: 0.9 !important;
    }

    .name {
      fill: ${theme.colors.global.textDefault} !important;
    }

    path {
      stroke: ${theme.colors.global.contentBackground} !important;
    }
  }
`);

export type OnClickMarkerEvent = {
  x: string,
  y: string,
}

export type OnHoverMarkerEvent = {
  positionX: number,
  positionY: number,
  x: string,
  y: string,
}

type ChartMarker = {
  colors?: Array<string>,
  color?: string,
  size?: number,
};

export type ChartConfig = {
  name: string,
  labels: Array<string>,
  originalLabels?: Array<string>,
  line?: ChartMarker,
  marker?: ChartMarker,
  originalName?: string,
};

export type ChartColor = {
  line?: ChartMarker,
  marker?: ChartMarker,
  outsidetextfont?: {
    color: string,
  },
};

type Props = {
  chartData: Array<any>,
  layout?: Partial<PlotLayout>,
  onZoom?: (from: string, to: string) => void,
  setChartColor?: (data: ChartConfig, color: ColorMapper) => ChartColor,
  onClickMarker?: (event: OnClickMarkerEvent) => void
  onHoverMarker?: (event: OnHoverMarkerEvent) => void,
  onUnhoverMarker?: () => void,
  onAfterPlot?: () => void,
};

type Axis = {
  autosize: boolean,
};

const nonInteractiveLayout = {
  yaxis: { fixedrange: true },
  xaxis: { fixedrange: true },
  hovermode: false,
};

const style = { height: '100%', width: '100%' };

const config = { displayModeBar: false, doubleClick: false as const, responsive: true };

const usePlotLayout = (layout: Partial<Layout>) => {
  const theme = useTheme();
  const interactive = useContext(InteractiveContext);
  const { colors } = useContext(ChartColorContext);

  return useMemo(() => {
    const fontSettings = {
      color: theme.colors.global.textDefault,
      size: ROOT_FONT_SIZE * Number(theme.fonts.size.small.replace(/rem|em/i, '')),
      family: theme.fonts.family.body,
    };

    const defaultLayout = {
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

    plotLayout.shapes = plotLayout.shapes.map((shape) => ({
      ...shape,
      line: { color: shape?.line?.color || colors.get(eventsDisplayName, EVENT_COLOR) },
    }));

    return interactive ? plotLayout : merge({}, plotLayout, nonInteractiveLayout);
  }, [colors, interactive, layout, theme]);
};

const usePlotChartData = (chartData: Array<any>, setChartColor: (data: ChartConfig, color: ColorMapper) => ChartColor) => {
  const theme = useTheme();
  const { colors } = useContext(ChartColorContext);

  return useMemo(() => chartData.map((chart) => {
    if (setChartColor && colors) {
      const conf = setChartColor(chart, colors);

      conf.outsidetextfont = { color: theme.colors.global.textDefault };

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
  }), [chartData, colors, setChartColor, theme.colors.global.textDefault]);
};

const GenericPlot = ({ chartData, layout = {}, setChartColor, onClickMarker = () => {}, onHoverMarker = () => {}, onUnhoverMarker = () => {}, onZoom = () => {}, onAfterPlot = () => {} }: Props) => {
  const interactive = useContext(InteractiveContext);
  const plotLayout = usePlotLayout(layout);
  const plotChartData = usePlotChartData(chartData, setChartColor);
  const onRenderComplete = useContext(RenderCompletionCallback);

  const _onRelayout = useCallback((axis: Axis) => {
    if (!axis.autosize && axis['xaxis.range[0]'] && axis['xaxis.range[1]']) {
      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      onZoom(from, to);
    }
  }, [onZoom]);

  const _onHoverMarker = useCallback((event: unknown) => {
    const { points } = event as { points: Array<{ bbox: { x0: number, y0: number }, y: string, x: string }> };

    onHoverMarker?.({
      positionX: points[0].bbox.x0,
      positionY: points[0].bbox.y0,
      x: points[0].x,
      y: points[0].y,
    });
  }, [onHoverMarker]);

  const _onMarkerClick = useCallback(({ points }: Readonly<Plotly.PlotMouseEvent>) => {
    onClickMarker?.({
      x: points[0].x as string,
      y: points[0].y as string,
    });
  }, [onClickMarker]);

  const _onAfterPlot = useCallback(() => {
    onRenderComplete();
    onAfterPlot();
  }, [onRenderComplete, onAfterPlot]);

  return (
    <StyledPlot data={plotChartData}
                useResizeHandler
                layout={plotLayout}
                style={style}
                onAfterPlot={_onAfterPlot}
                onClick={interactive ? _onMarkerClick : () => false}
                onHover={_onHoverMarker}
                onUnhover={onUnhoverMarker}
                onRelayout={interactive ? _onRelayout : () => {}}
                config={config} />
  );
};

export default GenericPlot;
