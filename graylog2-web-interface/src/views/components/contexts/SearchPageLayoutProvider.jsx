// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';

import SearchPageLayoutContext from './SearchPageLayoutContext';

const defaultLayout = {
  sidebar: {
    pinned: false,
  },
};

const SearchPageLayoutProvider = ({ children }: { children: React.Node }) => {
  const [layout, setLayout] = useState(defaultLayout);
  return (
    <SearchPageLayoutContext.Provider value={{ layout, setLayout }}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
};

SearchPageLayoutProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SearchPageLayoutProvider;
