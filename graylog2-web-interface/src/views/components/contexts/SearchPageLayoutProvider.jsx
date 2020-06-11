// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';

import SearchPageLayoutContext from './SearchPageLayoutContext';

const defaultLayoutConfig = {
  sidebar: {
    isInline: false,
  },
};

const SearchPageLayoutProvider = ({ children }: { children: React.Node }) => {
  const [config, setConfig] = useState(defaultLayoutConfig);
  return (
    <SearchPageLayoutContext.Provider value={{ config, setConfig }}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
};

SearchPageLayoutProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SearchPageLayoutProvider;
