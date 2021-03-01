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

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

import { PREFERENCES_THEME_MODE, DEFAULT_THEME_MODE, ThemeMode } from './constants';

import UserPreferencesContext from '../contexts/UserPreferencesContext';
import usePrefersColorScheme from '../hooks/usePrefersColorScheme';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { PreferencesStore } = CombinedProvider.get('Preferences');

type CurrentUser = {
  currentUser?: {
    username: string;
    read_only: boolean;
  };
};

const useCurrentThemeMode = () => {
  const browserThemePreference = usePrefersColorScheme();

  const { userIsReadOnly, username } = useStore(CurrentUserStore, (userStore) => ({
    username: (userStore as CurrentUser)?.currentUser?.username,
    userIsReadOnly: (userStore as CurrentUser)?.currentUser?.read_only ?? true,
  }));

  const userPreferences = useContext(UserPreferencesContext);
  const userThemePreference = userPreferences[PREFERENCES_THEME_MODE] ?? Store.get(PREFERENCES_THEME_MODE);
  const initialThemeMode = userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
  const [currentThemeMode, setCurrentThemeMode] = useState<ThemeMode>(initialThemeMode);

  const changeCurrentThemeMode = useCallback((newThemeMode: ThemeMode) => {
    setCurrentThemeMode(newThemeMode);
    Store.set(PREFERENCES_THEME_MODE, newThemeMode);

    if (!userIsReadOnly) {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: newThemeMode };

      PreferencesStore.saveUserPreferences(username, nextPreferences);
    }
  }, [userIsReadOnly, userPreferences, username]);

  return [currentThemeMode, changeCurrentThemeMode] as const;
};

export default useCurrentThemeMode;
