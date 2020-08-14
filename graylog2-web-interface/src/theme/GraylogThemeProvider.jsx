// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { useStore } from 'stores/connect';
import Store from 'logic/local-storage/Store';
import { breakpoints, colors, fonts, utils } from 'theme';
import type { ThemeInterface } from 'theme';
import usePrefersColorScheme from 'hooks/usePrefersColorScheme';
import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import StoreProvider from 'injection/StoreProvider';
import CombinedProvider from 'injection/CombinedProvider';

import { PREFERENCES_THEME_MODE, DEFAULT_THEME_MODE } from './constants';

type Props = {
  children: React.Node,
};

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const PreferencesStore = StoreProvider.getStore('Preferences');

const GraylogThemeProvider = ({ children }: Props) => {
  const colorScheme = usePrefersColorScheme();
  const [theme, setTheme] = useState();
  const [userPreferences, setUserPreferences] = useState();
  const [themeColors, setThemeColors] = useState();
  const [mode, setMode] = useState(colorScheme);

  const currentUser = useStore(CurrentUserStore, (userStore) => {
    setUserPreferences(userStore?.currentUser?.preferences);

    return userStore?.currentUser;
  });

  const currentUserThemeMode = () => {
    // eslint-disable-next-line camelcase
    return currentUser?.read_only ? Store.get(PREFERENCES_THEME_MODE) : (userPreferences && userPreferences[PREFERENCES_THEME_MODE]);
  };

  const handleModeChange = (nextMode) => Promise.resolve().then(() => {
    if (colors[nextMode]) {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: nextMode };

      setUserPreferences(nextPreferences);

      // eslint-disable-next-line camelcase
      if (currentUser?.read_only) {
        Store.set(PREFERENCES_THEME_MODE, nextMode);
      } else {
        PreferencesStore.saveUserPreferences(currentUser.username, PreferencesStore.convertPreferenceMapToArray(nextPreferences));
      }

      return { [PREFERENCES_THEME_MODE]: nextMode };
    }

    return null;
  });

  useEffect(() => {
    const hasCurrentThemeMode = currentUserThemeMode();

    if (hasCurrentThemeMode) {
      setMode(hasCurrentThemeMode);
    } else if (colorScheme !== mode) {
      setMode(colorScheme);
    } else {
      setMode(DEFAULT_THEME_MODE);
    }
  }, [colorScheme, userPreferences]);

  useEffect(() => {
    if (mode) {
      setThemeColors(colors[mode]);
    }
  }, [mode]);

  useEffect(() => {
    if (themeColors) {
      const formattedUtils = {
        ...utils,
        colorLevel: utils.colorLevel(themeColors),
        readableColor: utils.readableColor(themeColors),
      };

      const nextTheme: ThemeInterface = {
        mode,
        changeMode: handleModeChange,
        breakpoints,
        colors: themeColors,
        fonts,
        components: {
          button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
          aceEditor: aceEditorStyles({ colors: themeColors }),
        },
        utils: formattedUtils,
      };

      setTheme(nextTheme);
    }
  }, [themeColors]);

  return theme ? (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  ) : null;
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
