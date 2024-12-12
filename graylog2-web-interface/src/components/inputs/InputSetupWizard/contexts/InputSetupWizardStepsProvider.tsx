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

import * as React from 'react';
import { useMemo, useState } from 'react';

import InputSetupWizardStepsContext from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardStepsContext';
import type { StepsData } from 'components/inputs/InputSetupWizard/types';

const DEFAULT_STEPS_DATA = {};

const InputSetupWizardStepsProvider = ({ children = null }: React.PropsWithChildren<{}>) => {
  const [stepsData, setStepsData] = useState<StepsData>(DEFAULT_STEPS_DATA);

  const value = useMemo(() => ({
    stepsData,
    setStepsData,
  }), [
    stepsData,
  ]);

  return (
    <InputSetupWizardStepsContext.Provider value={value}>
      {children}
    </InputSetupWizardStepsContext.Provider>
  );
};

export default InputSetupWizardStepsProvider;
