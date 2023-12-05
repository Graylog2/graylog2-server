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
import * as React from 'react';
import type { PropsWithChildren } from 'react';
import { useCallback, useContext, useMemo, useState } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import { useStore } from 'stores/connect';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import { PreferencesStore } from 'stores/users/PreferencesStore';
import Store from 'logic/local-storage/Store';

import PerspectivesContext from './PerspectivesContext';

const usePersistedSetting = (settingKey: string) => {
  const { userIsReadOnly, username } = useStore(CurrentUserStore, (userStore) => ({
    username: userStore.currentUser?.username,
    userIsReadOnly: userStore.currentUser?.read_only ?? true,
  }));

  const userPreferences = useContext(UserPreferencesContext);
  const setting = userIsReadOnly ? Store.get(settingKey) : userPreferences[settingKey];

  const setSetting = useCallback((newSetting: string) => {
    if (userIsReadOnly) {
      Store.set(settingKey, newSetting);

      return Promise.resolve();
    }

    const nextPreferences = { ...userPreferences, [settingKey]: newSetting };

    return PreferencesStore.saveUserPreferences(username, nextPreferences);
  }, [settingKey, userIsReadOnly, userPreferences, username]);

  return useMemo(() => [setting, setSetting], [setSetting, setting]);
};

const PerspectivesProvider = ({ children }: PropsWithChildren) => {
  const [persistedPerspective, setPersistedPerspective] = usePersistedSetting('perspective');
  const [activePerspective, setActivePerspective] = useState(persistedPerspective ?? 'default');
  const setActivePerspectiveWithPersistence = useCallback((newPerspective: string) => {
    setActivePerspective(newPerspective);

    return setPersistedPerspective(newPerspective);
  }, [setPersistedPerspective]);
  const allPerspectives = usePluginEntities('perspectives');
  const availablePerspectives = allPerspectives
    .filter((perspective) => (perspective.useCondition ? !!perspective.useCondition() : true));

  const contextValue = useMemo(() => ({
    activePerspective,
    availablePerspectives,
    setActivePerspective: setActivePerspectiveWithPersistence,
  }), [activePerspective, availablePerspectives, setActivePerspectiveWithPersistence]);

  return (
    <PerspectivesContext.Provider value={contextValue}>
      {children}
    </PerspectivesContext.Provider>
  );
};

export default PerspectivesProvider;
