import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const ScratchpadContext = createContext();

export const ScratchpadProvider = ({ children }) => {
  const [isScratchpadVisible, setScratchpadVisibility] = useState(false);

  const toggleScratchpadVisibility = () => {
    setScratchpadVisibility(!isScratchpadVisible);
  };

  return (
    <ScratchpadContext.Provider value={{
      isScratchpadVisible,
      setScratchpadVisibility,
      toggleScratchpadVisibility,
    }}>
      {children}
    </ScratchpadContext.Provider>
  );
};

ScratchpadProvider.propTypes = {
  children: PropTypes.node.isRequired,
};
