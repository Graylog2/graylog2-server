/* eslint-disable camelcase */
import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { breakpoints, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import CombinedProvider from 'injection/CombinedProvider';
import CustomizationContext from 'contexts/CustomizationContext';

import { CUSTOMIZATION_THEME_MODE, THEME_MODE_LIGHT } from './constants';

const { CustomizationsActions } = CombinedProvider.get('Customizations');

const loadTheme = (mode) => (
  import(`theme/variants/${mode}.js`)
    .then((modeColors) => {
      return modeColors.default;
    })
    .catch((error) => {
      console.error('loading colors failed: ', error);
    })
);

const GraylogThemeProvider = ({ children }) => {
  useEffect(() => {
    CustomizationsActions.get(CUSTOMIZATION_THEME_MODE);
  }, []);

  const [colors, setColors] = useState(null);
  const themeMode = useContext(CustomizationContext)[CUSTOMIZATION_THEME_MODE];
  const mode = themeMode?.theme_mode || THEME_MODE_LIGHT;

  loadTheme(mode).then((modeColor) => {
    setColors(modeColor);
  });

  if (!colors) { return null; }

  return (
    <ThemeProvider theme={{
      mode,
      breakpoints,
      colors,
      fonts,
      components: {
        button: buttonStyles({ colors, utils }),
        aceEditor: aceEditorStyles({ colors }),
      },
      utils,
    }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
