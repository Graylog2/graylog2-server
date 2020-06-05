// @flow strict
import * as React from 'react';

type SearchPageLayoutType = {
  layout: {
    sidebar: { pinned: boolean },
  },
  setLayout: (tbd: any) => void,
};

const SearchPageLayoutContext = React.createContext<?SearchPageLayoutType>();

export default SearchPageLayoutContext;
