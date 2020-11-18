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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutState from './SearchPageLayoutState';

type Props = {
  children: React.ReactElement,
};

const SearchPageLayoutProvider = ({ children }: Props) => {
  return (
    <SearchPageLayoutState>
      {({ getLayoutState, setLayoutState }) => {
        const config = {
          sidebar: { isPinned: getLayoutState('sidebarIsPinned', false) },
        };
        const actions = { toggleSidebarPinning: () => setLayoutState('sidebarIsPinned', !config.sidebar.isPinned) };

        return (
          <SearchPageLayoutContext.Provider value={{ config, actions }}>
            {children}
          </SearchPageLayoutContext.Provider>
        );
      }}

    </SearchPageLayoutState>
  );
};

SearchPageLayoutProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SearchPageLayoutProvider;
