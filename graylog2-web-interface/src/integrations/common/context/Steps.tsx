import React, { createContext, useCallback, useMemo, useState } from 'react';

import type { StepsContextType, isDisabledStepType, SetEnabledStepType } from '../utils/types';

export const StepsContext = createContext<StepsContextType | null>(null);

export const StepsProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const [currentStep, setCurrentStep] = useState<string>('authorize');
  const [enabledSteps, enableStep] = useState<string[]>(['authorize']);
  const [availableSteps, setAvailableStep] = useState<string[]>([]);

  const isDisabledStep: isDisabledStepType = useCallback(
    (step) => {
      if (!enabledSteps || enabledSteps.length === 0) {
        return true;
      }

      return !enabledSteps.includes(step);
    },
    [enabledSteps],
  );

  const setEnabledStep: SetEnabledStepType = useCallback(
    (step) => {
      enableStep([...enabledSteps, step]);
    },
    [enabledSteps],
  );

  const stepContextProviderValue = useMemo(
    () => ({
      availableSteps,
      currentStep,
      enabledSteps,
      isDisabledStep,
      setAvailableStep,
      setCurrentStep,
      setEnabledStep,
    }),
    [availableSteps, currentStep, enabledSteps, isDisabledStep, setEnabledStep],
  );

  return <StepsContext.Provider value={stepContextProviderValue}>{children}</StepsContext.Provider>;
};
