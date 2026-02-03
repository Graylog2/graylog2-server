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
import { useCallback, useContext, useMemo } from 'react';

import type { UserPreferences } from 'contexts/UserPreferencesContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import { PreferencesStore } from 'stores/users/PreferencesStore';
import Store from 'logic/local-storage/Store';
import useCurrentUser from 'hooks/useCurrentUser';

const usePersistedSetting = <T extends keyof UserPreferences>(
  settingKey: T,
): [UserPreferences[T], (newValue: UserPreferences[T]) => void] => {
  const currentUser = useCurrentUser(false);
  const { userIsReadOnly, username } = useMemo(
    () => ({ username: currentUser?.username, userIsReadOnly: currentUser?.readOnly ?? true }),
    [currentUser],
  );

  const userPreferences = useContext(UserPreferencesContext);
  const setting: UserPreferences[T] = userIsReadOnly ? Store.get(settingKey) : userPreferences[settingKey];

  const setSetting = useCallback(
    (newSetting: UserPreferences[T]) => {
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
