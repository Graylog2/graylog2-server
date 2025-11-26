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
const getDefaultPlotYLayoutSettings = (theme: DefaultTheme, visualizationAxisTitle: string = '') => {
  const fontSettings = getDefaultPlotFontSettings(theme);

  return {
    automargin: true,
    gridcolor: theme.colors.variant.lightest.default,
    tickfont: fontSettings,
    title: {
      font: fontSettings,
      text: visualizationAxisTitle,
      automargin: true,
      standoff: AXIS_LABEL_MARGIN,
    },
  };
};

export default getDefaultPlotYLayoutSettings;
