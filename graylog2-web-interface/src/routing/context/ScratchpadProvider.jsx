
import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const ScratchpadContext = createContext();

export const ScratchpadProvider = ({ children }) => {
  const [isScratchpadVisible, setScratchpadVisibility] = useState(false);

  return (
    <ScratchpadContext.Provider value={{
      isScratchpadVisible,
      setScratchpadVisibility,
    }}>
      {children}
    </ScratchpadContext.Provider>
  );
};

ScratchpadProvider.propTypes = {
  children: PropTypes.node.isRequired,
};
