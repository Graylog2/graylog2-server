// @flow strict
import { useCallback, useContext, useState } from 'react';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

import { PREFERENCES_THEME_MODE, DEFAULT_THEME_MODE } from './constants';

import UserPreferencesContext from '../contexts/UserPreferencesContext';
import usePrefersColorScheme from '../hooks/usePrefersColorScheme';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { PreferencesStore } = CombinedProvider.get('Preferences');

const useCurrentThemeMode = () => {
  const browserThemePreference = usePrefersColorScheme();

  const { userIsReadOnly, username } = useStore(CurrentUserStore, (userStore) => ({
    username: userStore?.currentUser?.username,
    // eslint-disable-next-line camelcase
    userIsReadOnly: userStore?.currentUser?.read_only ?? true,
  }));

  const userPreferences = useContext(UserPreferencesContext);
  const userThemePreference = userPreferences[PREFERENCES_THEME_MODE] ?? Store.get(PREFERENCES_THEME_MODE);
  const initialThemeMode = userThemePreference ?? browserThemePreference ?? DEFAULT_THEME_MODE;
  const [currentThemeMode, setCurrentThemeMode] = useState<string>(initialThemeMode);

  const changeCurrentThemeMode = useCallback((newThemeMode: string) => {
    setCurrentThemeMode(newThemeMode);
    Store.set(PREFERENCES_THEME_MODE, newThemeMode);

    if (!userIsReadOnly) {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: newThemeMode };

      PreferencesStore.saveUserPreferences(username, nextPreferences);
    }
  }, [userIsReadOnly, userPreferences, username]);

  return [currentThemeMode, changeCurrentThemeMode];
};

export default useCurrentThemeMode;
