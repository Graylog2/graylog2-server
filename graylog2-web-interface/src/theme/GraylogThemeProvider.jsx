import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import CombinedProvider from 'injection/CombinedProvider';
import { breakpoints, colors as themeColors, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import CustomizationContext from 'contexts/CustomizationContext';

import { CUSTOMIZATION_THEME_MODE, THEME_MODE_LIGHT } from './constants';

const { CustomizationsActions } = CombinedProvider.get('Customizations');

/* NOTE: mode will eventually need to come from User Preferences */
const updateThemeMode = (theme_mode) => CustomizationsActions.update(CUSTOMIZATION_THEME_MODE, { theme_mode });

const GraylogThemeProvider = ({ children }) => {
  const themeMode = useContext(CustomizationContext)[CUSTOMIZATION_THEME_MODE];
  const mode = themeMode?.theme_mode || THEME_MODE_LIGHT;
  console.log('GraylogThemeProvider', mode, themeColors);

  const colors = themeColors[mode];

  return (
    <ThemeProvider theme={{
      mode,
      updateThemeMode,
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
