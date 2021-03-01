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
import { useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider, DefaultTheme } from 'styled-components';
import merge from 'lodash/merge';
import { $PropertyType } from 'utility-types';

import { breakpoints, colors, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import AppConfig from 'util/AppConfig';
import UpdatableThemeContext from 'theme/UpdatableThemeContext';
import { generateGrayScale, generateInputColors, generateTableColors, generateVariantColors, generateGlobalColors } from 'theme/variants/util';
import { Colors, ThemeColorModes } from 'theme/colors';

import useCurrentThemeMode from './UseCurrentThemeMode';

const customizedTheme = AppConfig.customTheme();

const generateTheme = ({ changeMode, customizedThemeColors, mode }): DefaultTheme => {
  let currentTheme: Colors = colors[mode];

  let tableColors: $PropertyType<Colors, 'table'> = colors[mode].table;
  let inputColors: $PropertyType<Colors, 'input'> = colors[mode].input;
  let variantColors: $PropertyType<Colors, 'variant'> = colors[mode].variant;
  let grayColors: $PropertyType<Colors, 'gray'> = colors[mode].gray;
  let globalColors: $PropertyType<Colors, 'global'> = colors[mode].global;

  if (customizedThemeColors && Object.entries(customizedThemeColors).length > 0) {
    currentTheme = merge({}, colors, customizedThemeColors)[mode];

    tableColors = generateTableColors(mode, currentTheme.variant);
    inputColors = generateInputColors(mode, currentTheme.global, currentTheme.gray, currentTheme.variant);
    grayColors = generateGrayScale(currentTheme.global.textDefault, currentTheme.global.textAlt);

    globalColors = {
      ...currentTheme.global,
      ...generateGlobalColors(mode, currentTheme.brand, currentTheme.global, currentTheme.variant),
    };

    variantColors = {
      ...currentTheme.variant,
      ...generateVariantColors(mode, currentTheme.variant),
    };
  }

  const currentThemeColors = {
    ...currentTheme,
    gray: grayColors,
    global: globalColors,
    input: inputColors,
    table: tableColors,
    variant: variantColors,
  };

  const formattedUtils = {
    ...utils,
    colorLevel: utils.colorLevel(currentThemeColors),
    readableColor: utils.readableColor(currentThemeColors),
  };

  return {
    mode,
    changeMode,
    breakpoints,
    colors: currentThemeColors,
    fonts,
    components: {
      button: buttonStyles({ colors: currentThemeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: currentThemeColors }),
    },
    utils: formattedUtils,
  };
};

const GraylogThemeProvider = ({ children }) => {
  const [canCustomizeTheme, setCanCustomizeTheme] = useState<boolean>(false);
  const [mode, changeMode] = useCurrentThemeMode();
  const hasEnterprisePlugin = true; // TODO: get from source
  const [customizedThemeColors, setCustomizedThemeColors] = useState<ThemeColorModes | undefined>(canCustomizeTheme ? customizedTheme : {});

  useEffect(() => {
    if (hasEnterprisePlugin) {
      setCanCustomizeTheme(true);
    }
  }, [hasEnterprisePlugin]);

  const updatableTheme = (newColors: any) => {
    setCustomizedThemeColors(newColors);
  };

  const theme = useMemo(() => generateTheme({ mode, changeMode, customizedThemeColors }),
    [mode, customizedThemeColors, changeMode]);

  return (
    <UpdatableThemeContext.Provider value={{ updatableTheme }}>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
    </UpdatableThemeContext.Provider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default GraylogThemeProvider;
