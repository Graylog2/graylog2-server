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

import { AXIS_LABEL_MARGIN } from 'views/components/visualizations/Constants';
import getDefaultPlotFontSettings from 'views/components/visualizations/utils/getDefaultPlotFontSettings';
import type { ChartAxisConfig } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

const getDefaultPlotXLayoutSettings = (theme: DefaultTheme, config?: AggregationWidgetConfig) => {
  const axisConfig =
    config?.visualizationConfig && 'axisConfig' in config.visualizationConfig
      ? (config.visualizationConfig.axisConfig as ChartAxisConfig)?.xaxis
      : undefined;
  const fontSettings = getDefaultPlotFontSettings(theme);
  const color = axisConfig?.color ?? fontSettings.color;

  return {
    automargin: true,
    gridcolor: theme.colors.charts.lines,
    zerolinecolor: theme.colors.charts.zeroLine,
    tickfont: fontSettings,
    title: {
      font: { ...fontSettings, color },
      text: axisConfig?.title,
      standoff: AXIS_LABEL_MARGIN,
    },
  };
};

export default getDefaultPlotXLayoutSettings;
