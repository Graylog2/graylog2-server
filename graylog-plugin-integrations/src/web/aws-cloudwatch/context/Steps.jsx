
import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const StepsContext = createContext();

export const StepsProvider = ({ children }) => {
  const [currentStep, setCurrentStep] = useState('authorize');
  const [enabledSteps, enableStep] = useState(['authorize']);
  const [availableSteps, setAvailableStep] = useState([]);

  const isDisabledStep = (step) => {
    if (!enabledSteps || enabledSteps.length === 0) {
      return true;
    }

    return !enabledSteps.includes(step);
  };

  const setEnabledStep = (step) => {
    enableStep([...enabledSteps, step]);
  };

  return (
    <StepsContext.Provider value={{
      availableSteps,
      currentStep,
      enabledSteps,
      isDisabledStep,
      setAvailableStep,
      setCurrentStep,
      setEnabledStep,
    }}>
      {children}
    </StepsContext.Provider>
  );
};

StepsProvider.propTypes = {
  children: PropTypes.any.isRequired,
};
