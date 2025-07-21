import { useCallback, useContext, useMemo } from 'react';

import UserPreferencesContext from 'contexts/UserPreferencesContext';
import { PreferencesStore } from 'stores/users/PreferencesStore';
import Store from 'logic/local-storage/Store';
import useCurrentUser from 'hooks/useCurrentUser';

const usePersistedSetting = (settingKey: string) => {
  const currentUser = useCurrentUser();
  const { userIsReadOnly, username } = useMemo(
    () => ({ username: currentUser?.username, userIsReadOnly: currentUser?.readOnly ?? true }),
    [currentUser],
  );

  const userPreferences = useContext(UserPreferencesContext);
  const setting = userIsReadOnly ? Store.get(settingKey) : userPreferences[settingKey];

  const setSetting = useCallback(
    (newSetting: string) => {
      if (userIsReadOnly) {
        Store.set(settingKey, newSetting);

        return Promise.resolve();
      }

      const nextPreferences = { ...userPreferences, [settingKey]: newSetting };

      return PreferencesStore.saveUserPreferences(username, nextPreferences);
    },
    [settingKey, userIsReadOnly, userPreferences, username],
  );

  return useMemo(() => [setting, setSetting], [setSetting, setting]);
};

export default usePersistedSetting;
