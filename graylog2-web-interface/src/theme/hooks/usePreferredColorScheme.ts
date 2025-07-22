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
import { useCallback, useState } from 'react';
import type { ColorScheme } from '@graylog/sawmill';

import Store from 'logic/local-storage/Store';
import type { UserPreferences } from 'contexts/UserPreferencesContext';
import usePersistedSetting from 'hooks/usePersistedSetting';

import useBrowserColorSchemePreference from './useBrowserColorSchemePreference';

import type { LegacyColorScheme } from '../constants';
import {
  COLOR_SCHEME_DARK,
  COLOR_SCHEME_LIGHT,
  DEFAULT_THEME_MODE,
  LEGACY_COLOR_SCHEME_DARK,
  LEGACY_COLOR_SCHEME_LIGHT,
  PREFERENCES_THEME_MODE,
} from '../constants';

const fromLegacyColorSchemeName = (legacyColorScheme: LegacyColorScheme): ColorScheme => {
  if (legacyColorScheme === LEGACY_COLOR_SCHEME_LIGHT) {
    return COLOR_SCHEME_LIGHT;
  }

  if (legacyColorScheme === LEGACY_COLOR_SCHEME_DARK) {
    return COLOR_SCHEME_DARK;
  }

  return legacyColorScheme;
};

const getInitialThemeMode = ({
  userPreferencesThemeMode,
  browserThemePreference,
  initialThemeModeOverride,
  userIsLoggedIn,
}: {
  userPreferencesThemeMode: UserPreferences[typeof PREFERENCES_THEME_MODE];
  browserThemePreference: ColorScheme;
  initialThemeModeOverride: ColorScheme;
  userIsLoggedIn: boolean;
}) => {
  if (initialThemeModeOverride) {
    return initialThemeModeOverride;
  }

  if (!userIsLoggedIn) {
    return DEFAULT_THEME_MODE;
  }

  const userThemePreference = userPreferencesThemeMode ?? fromLegacyColorSchemeName(Store.get(PREFERENCES_THEME_MODE));

  return userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
};

const usePreferredColorScheme = (
  initialThemeModeOverride: ColorScheme,
  userIsLoggedIn: boolean,
): [ColorScheme, (newThemeMode: ColorScheme) => void] => {
  const browserThemePreference = useBrowserColorSchemePreference();
  const [themeMode, setThemeMode] = usePersistedSetting(PREFERENCES_THEME_MODE);

  const [currentThemeMode, setCurrentThemeMode] = useState<ColorScheme>(() =>
    getInitialThemeMode({
      userPreferencesThemeMode: themeMode,
      browserThemePreference,
      initialThemeModeOverride,
      userIsLoggedIn,
    }),
  );

  const changeCurrentThemeMode = useCallback(
    (newThemeMode: ColorScheme) => {
      setCurrentThemeMode(newThemeMode);

      if (userIsLoggedIn) {
        setThemeMode(newThemeMode);
      }
    },
    [setThemeMode, userIsLoggedIn],
  );

  return [currentThemeMode, changeCurrentThemeMode];
};

export default usePreferredColorScheme;
