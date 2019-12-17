
import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const AdvancedOptionsContext = createContext();

export const AdvancedOptionsProvider = ({ children }) => {
  const [isAdvancedOptionsVisible, setAdvancedOptionsVisibility] = useState(false);
  const [isAWSCustomEndpointsVisible, setAWSCustomEndpointsVisibility] = useState(false);

  return (
    <AdvancedOptionsContext.Provider value={{
      isAdvancedOptionsVisible,
      isAWSCustomEndpointsVisible,
      setAdvancedOptionsVisibility,
      setAWSCustomEndpointsVisibility,
    }}>
      {children}
    </AdvancedOptionsContext.Provider>
  );
};

AdvancedOptionsProvider.propTypes = {
  children: PropTypes.any.isRequired,
};
