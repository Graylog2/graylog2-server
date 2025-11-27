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

import getDefaultPlotFontSettings from 'views/components/visualizations/utils/getDefaultPlotFontSettings';
import { AXIS_LABEL_MARGIN } from 'views/components/visualizations/Constants';
const getDefaultPlotYLayoutSettings = (
  theme: DefaultTheme,
  visualizationAxisTitle: string = '',
  visualizationAxisColor: string = undefined,
) => {
  const fontSettings = getDefaultPlotFontSettings(theme);
  const color = visualizationAxisColor ?? fontSettings.color;
  const gridColor = visualizationAxisColor
    ? theme.utils.colorLevel(visualizationAxisColor, -4)
    : theme.colors.variant.lightest.default;

  return {
    automargin: true,
    gridcolor: gridColor,
    tickfont: { ...fontSettings, color },
    title: {
      font: { ...fontSettings, color },
      text: visualizationAxisTitle,
      automargin: true,
      standoff: AXIS_LABEL_MARGIN,
    },
  };
};

export default getDefaultPlotYLayoutSettings;
