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
import { useMemo } from 'react';

import SearchPagePreferencesContext from './SearchPagePreferencesContext';
import SearchPagePreferencesState from './SearchPagePreferencesState';

type Props = {
  children: React.ReactNode
  setPreferences: (stateKey: string, value: boolean) => void,
  getPreferences: (stateKey: string, defaultValue: boolean) => boolean,
};

const SearchPagePreferencesStateProvider = ({ getPreferences, setPreferences, children }: Props) => {
  const searchPagePreferencesContextValue = useMemo(() => {
    const config = {
      sidebar: { isPinned: getPreferences('sidebarIsPinned', false) },
    };
    const actions = { toggleSidebarPinning: () => setPreferences('sidebarIsPinned', !config.sidebar.isPinned) };

    return ({
      config,
      actions,
    });
  }, [getPreferences, setPreferences]);

  return (
    <SearchPagePreferencesContext.Provider value={searchPagePreferencesContextValue}>
      {children}
    </SearchPagePreferencesContext.Provider>
  );
};

const SearchPagePreferencesProvider = ({ children }: { children: React.ReactNode }) => (
  <SearchPagePreferencesState>
    {({ getPreferences, setPreferences }) => (
      <SearchPagePreferencesStateProvider getPreferences={getPreferences} setPreferences={setPreferences}>
        {children}
      </SearchPagePreferencesStateProvider>
    )}
  </SearchPagePreferencesState>
);

export default SearchPagePreferencesProvider;
