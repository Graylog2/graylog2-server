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
  setPreferencesState: (stateKey: string, value: boolean) => void,
  getPreferencesState: (stateKey: string, defaultValue: boolean) => boolean,
};

const SearchPagePreferencesProvider = ({ getPreferencesState, setPreferencesState, children }: Props) => {
  const searchPagePreferencesContextValue = useMemo(() => {
    const config = {
      sidebar: { isPinned: getPreferencesState('sidebarIsPinned', false) },
    };
    const actions = { toggleSidebarPinning: () => setPreferencesState('sidebarIsPinned', !config.sidebar.isPinned) };

    return ({
      config,
      actions,
    });
  }, [getPreferencesState, setPreferencesState]);

  return (
    <SearchPagePreferencesContext.Provider value={searchPagePreferencesContextValue}>
      {children}
    </SearchPagePreferencesContext.Provider>
  );
};

const SearchPagePreferencesStateProvider = ({ children }: { children: React.ReactNode }) => (
  <SearchPagePreferencesState>
    {({ getPreferencesState, setPreferencesState }) => (
      <SearchPagePreferencesProvider getPreferencesState={getPreferencesState} setPreferencesState={setPreferencesState}>
        {children}
      </SearchPagePreferencesProvider>
    )}
  </SearchPagePreferencesState>
);

export default SearchPagePreferencesStateProvider;
