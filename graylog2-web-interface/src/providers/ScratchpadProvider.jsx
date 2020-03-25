import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

import Store from 'logic/local-storage/Store';

export const ScratchpadContext = createContext();

export const ScratchpadProvider = ({ children, loginName }) => {
  const localStorageItem = `gl-scratchpad-${loginName}`;
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isScratchpadVisible, setVisibility] = useState(scratchpadStore.opened || false);

  const toggleScratchpadVisibility = () => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened: !isScratchpadVisible });
    setVisibility(!isScratchpadVisible);
  };

  const setScratchpadVisibility = (opened) => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened });
    setVisibility(opened);
  };

  return (
    <ScratchpadContext.Provider value={{
      isScratchpadVisible,
      localStorageItem,
      setScratchpadVisibility,
      toggleScratchpadVisibility,
    }}>
      {children}
    </ScratchpadContext.Provider>
  );
};

ScratchpadProvider.propTypes = {
  children: PropTypes.node.isRequired,
  loginName: PropTypes.string.isRequired,
};
