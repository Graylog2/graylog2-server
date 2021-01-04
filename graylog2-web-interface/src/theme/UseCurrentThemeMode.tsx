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

import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

import { PREFERENCES_THEME_MODE, DEFAULT_THEME_MODE } from './constants';

import UserPreferencesContext from '../contexts/UserPreferencesContext';
import usePrefersColorScheme from '../hooks/usePrefersColorScheme';
import CurrentUserContext from '../contexts/CurrentUserContext';

const { PreferencesStore } = CombinedProvider.get('Preferences');

type CurrentUser = {
  currentUser?: {
    username: string;
    read_only: boolean;
  };
};

const useCurrentThemeMode = (): [string, (newThemeMode: string) => void] => {
  const browserThemePreference = usePrefersColorScheme();
  const userStore = useContext(CurrentUserContext);

  const userIsReadOnly = (userStore as CurrentUser)?.currentUser?.read_only ?? true;
  const username = (userStore as CurrentUser)?.currentUser?.username;

  const userPreferences = useContext(UserPreferencesContext);
  const userThemePreference = userPreferences[PREFERENCES_THEME_MODE] ?? Store.get(PREFERENCES_THEME_MODE);
  const initialThemeMode = userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
  const [currentThemeMode, setCurrentThemeMode] = useState<string>(initialThemeMode);

  const changeCurrentThemeMode = useCallback((newThemeMode: string) => {
    Store.set(PREFERENCES_THEME_MODE, newThemeMode);

    if (!userIsReadOnly) {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: newThemeMode };

      PreferencesStore.saveUserPreferences(username, nextPreferences);
    }

    setCurrentThemeMode(newThemeMode);
  }, [userIsReadOnly, userPreferences, username]);

  return [currentThemeMode, changeCurrentThemeMode];
};

export default useCurrentThemeMode;
