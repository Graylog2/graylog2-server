/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
