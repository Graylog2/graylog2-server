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
import { useCallback, useContext, useState } from 'react';
import type { ColorScheme } from '@graylog/sawmill';

import { useStore } from 'stores/connect';
import Store from 'logic/local-storage/Store';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { PreferencesStore } from 'stores/users/PreferencesStore';

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
import UserPreferencesContext from '../../contexts/UserPreferencesContext';

const fromLegacyColorSchemeName = (legacyColorScheme: LegacyColorScheme): ColorScheme => {
  if (legacyColorScheme === LEGACY_COLOR_SCHEME_LIGHT) {
    return COLOR_SCHEME_LIGHT;
  }

  if (legacyColorScheme === LEGACY_COLOR_SCHEME_DARK) {
    return COLOR_SCHEME_DARK;
  }

  return legacyColorScheme;
};

const getInitialThemeMode = (userPreferences: Record<typeof PREFERENCES_THEME_MODE, ColorScheme>, browserThemePreference: ColorScheme, initialThemeModeOverride: ColorScheme) => {
  if (initialThemeModeOverride) {
    return initialThemeModeOverride;
  }

  const userThemePreference = userPreferences[PREFERENCES_THEME_MODE] ?? fromLegacyColorSchemeName(Store.get(PREFERENCES_THEME_MODE));

  return userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
};

const usePreferredColorScheme = (initialThemeModeOverride: ColorScheme): [ColorScheme, (newThemeMode: ColorScheme) => void] => {
  const browserThemePreference = useBrowserColorSchemePreference();

  const { userIsReadOnly, username } = useStore(CurrentUserStore, (userStore) => ({
    username: userStore.currentUser?.username,
    userIsReadOnly: userStore.currentUser?.read_only ?? true,
  }));

  const userPreferences = useContext(UserPreferencesContext);
  const initialThemeMode = getInitialThemeMode(userPreferences, browserThemePreference, initialThemeModeOverride);
  const [currentThemeMode, setCurrentThemeMode] = useState<ColorScheme>(initialThemeMode);

  const changeCurrentThemeMode = useCallback((newThemeMode: ColorScheme) => {
    setCurrentThemeMode(newThemeMode);
    Store.set(PREFERENCES_THEME_MODE, newThemeMode);

    if (!userIsReadOnly) {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: newThemeMode };

      PreferencesStore.saveUserPreferences(username, nextPreferences);
    }
  }, [userIsReadOnly, userPreferences, username]);

  return [currentThemeMode, changeCurrentThemeMode];
};

export default usePreferredColorScheme;
