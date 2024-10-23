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
import React, { createContext, useState } from 'react';

// TODO: Fix typing
export const StepsContext = createContext<any>(undefined);

type StepsProviderProps = {
  children: any;
};

export const StepsProvider = ({
  children,
}: StepsProviderProps) => {
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
