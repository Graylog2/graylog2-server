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

import type { LegacyColorScheme } from './constants';
import {
  PREFERENCES_THEME_MODE,
  DEFAULT_THEME_MODE,
  LEGACY_COLOR_SCHEME_DARK,
  COLOR_SCHEME_DARK,
  COLOR_SCHEME_LIGHT,
} from './constants';

import UserPreferencesContext from '../contexts/UserPreferencesContext';
import usePrefersColorScheme from '../hooks/usePrefersColorScheme';

const modeFromPreferences = (userPreferences: Record<typeof PREFERENCES_THEME_MODE, LegacyColorScheme>) => {
  if (userPreferences[PREFERENCES_THEME_MODE]) {
    return userPreferences[PREFERENCES_THEME_MODE] === LEGACY_COLOR_SCHEME_DARK ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
  }

  return null;
};

const modeFromStore = () => {
  const mode = Store.get(PREFERENCES_THEME_MODE);

  if (mode) {
    return mode === LEGACY_COLOR_SCHEME_DARK ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
  }

  return null;
};

const getInitialThemeMode = (userPreferences: Record<typeof PREFERENCES_THEME_MODE, LegacyColorScheme>, browserThemePreference: ColorScheme, initialThemeModeOverride: ColorScheme) => {
  if (initialThemeModeOverride) {
    return initialThemeModeOverride;
  }

  const userThemePreference = modeFromPreferences(userPreferences) ?? modeFromStore();

  return userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
};

const usePreferredColorScheme = (initialThemeModeOverride: ColorScheme): [ColorScheme, (newThemeMode: ColorScheme) => void] => {
  const browserThemePreference = usePrefersColorScheme();

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
