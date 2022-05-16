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
import PropTypes from 'prop-types';

import SearchPagePreferencesContext from './SearchPagePreferencesContext';
import SearchPagePreferencesState from './SearchPagePreferencesState';

type Props = {
  children: React.ReactElement,
};

const SearchPagePreferencesProvider = ({ children }: Props) => {
  return (
    <SearchPagePreferencesState>
      {({ getPreferenceState, setPreferenceState }) => {
        const config = {
          sidebar: { isPinned: getPreferenceState('sidebarIsPinned', false) },
        };
        const actions = { toggleSidebarPinning: () => setPreferenceState('sidebarIsPinned', !config.sidebar.isPinned) };

        return (
          <SearchPagePreferencesContext.Provider value={{ config, actions }}>
            {children}
          </SearchPagePreferencesContext.Provider>
        );
      }}

    </SearchPagePreferencesState>
  );
};

SearchPagePreferencesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SearchPagePreferencesProvider;
