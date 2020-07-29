// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutState from './SearchPageLayoutState';

type Props = {
  children: React.Node,
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
