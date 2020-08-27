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
  const browserPreference = usePrefersColorScheme();
  const { userIsReadOnly, username } = useStore(CurrentUserStore, (userStore) => ({
    username: userStore?.currentUser?.username,
    // eslint-disable-next-line camelcase
    userIsReadOnly: userStore?.currentUser?.read_only ?? true,
  }));
  const userPreferences = useContext(UserPreferencesContext);
  const initialThemeMode = (userIsReadOnly ? Store.get(PREFERENCES_THEME_MODE) : userPreferences[PREFERENCES_THEME_MODE]) ?? browserPreference ?? DEFAULT_THEME_MODE;
  const [currentThemeMode, setCurrentThemeMode] = useState(initialThemeMode);

  const changeCurrentThemeMode = useCallback((newThemeMode: string) => {
    setCurrentThemeMode(newThemeMode);

    if (userIsReadOnly) {
      Store.set(PREFERENCES_THEME_MODE, newThemeMode);
    } else {
      const nextPreferences = { ...userPreferences, [PREFERENCES_THEME_MODE]: newThemeMode };

      PreferencesStore.saveUserPreferences(username, PreferencesStore.convertPreferenceMapToArray(nextPreferences));
    }
  }, [userIsReadOnly, userPreferences, username]);

  return [currentThemeMode, changeCurrentThemeMode];
};

export default useCurrentThemeMode;
