// @flow strict
import { useState } from 'react';

const SearchPageLayoutState = ({ children }) => {
  const [state, setState] = useState({});

  const getLayoutState = (stateKey, defaultValue) => {
    return state[stateKey] ?? defaultValue;
  };

  const setLayoutState = (stateKey, value) => {
    setState({ ...state, [stateKey]: value });
  };

  return children({ getLayoutState, setLayoutState });
};

export default SearchPageLayoutState;
