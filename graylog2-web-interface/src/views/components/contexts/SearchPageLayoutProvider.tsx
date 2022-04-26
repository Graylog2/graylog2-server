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

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutState from './SearchPageLayoutState';

type Props = {
  children: React.ReactNode
  setLayoutState: (stateKey: string, value: boolean) => void,
  getLayoutState: (stateKey: string, defaultValue: boolean) => boolean,
};

const SearchPageLayoutProvider = ({ getLayoutState, setLayoutState, children }: Props) => {
  const searchPageLayoutContextValue = useMemo(() => {
    const config = {
      sidebar: { isPinned: getLayoutState('sidebarIsPinned', false) },
    };
    const actions = { toggleSidebarPinning: () => setLayoutState('sidebarIsPinned', !config.sidebar.isPinned) };

    return ({
      config,
      actions,
    });
  }, [getLayoutState, setLayoutState]);

  return (
    <SearchPageLayoutContext.Provider value={searchPageLayoutContextValue}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
};

const SearchPageLayoutStateProvider = ({ children }: { children: React.ReactNode }) => (
  <SearchPageLayoutState>
    {({ getLayoutState, setLayoutState }) => (
      <SearchPageLayoutProvider getLayoutState={getLayoutState} setLayoutState={setLayoutState}>
        {children}
      </SearchPageLayoutProvider>
    )}
  </SearchPageLayoutState>
);

export default SearchPageLayoutStateProvider;
