/* eslint-disable camelcase */
import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import find from 'lodash/find';

import { breakpoints, colors, fonts, utils } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import StoreProvider from 'injection/StoreProvider';
import CurrentUserContext from 'contexts/CurrentUserContext';

import { PREFERENCES_THEME_MODE, THEME_MODE_LIGHT } from './constants';

const PreferencesStore = StoreProvider.getStore('Preferences');

const GraylogThemeProvider = ({ children }) => {
  const currentUser = useContext(CurrentUserContext);

  if (!currentUser) { return null; }

  const [mode, setMode] = useState(THEME_MODE_LIGHT);
  const themeColors = colors[mode];

  useEffect(() => {
    PreferencesStore.loadUserPreferences(currentUser.username, (preferences) => {
      const themePreferences = find(preferences, (pref) => pref.name === PREFERENCES_THEME_MODE);

      if (themePreferences) {
        setMode(themePreferences.value);
      }
    });
  }, [currentUser]);

  return (
    <ThemeProvider theme={{
      mode,
      breakpoints,
      colors: themeColors,
      fonts,
      components: {
        button: buttonStyles({ colors: themeColors, utils }),
        aceEditor: aceEditorStyles({ colors: themeColors }),
      },
      utils: {
        ...utils,
        colorLevel: utils.colorLevel(themeColors),
        readableColor: utils.readableColor(themeColors),
      },
    }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
