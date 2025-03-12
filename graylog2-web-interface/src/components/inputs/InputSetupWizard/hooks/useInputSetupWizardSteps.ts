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
import { useContext } from 'react';

import InputSetupWizardStepsContext from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardStepsContext';
import type { InputSetupWizardStepsContextType } from 'components/inputs/InputSetupWizard/contexts/InputSetupWizardStepsContext';

const useInputSetupWizardSteps = <StepsData>() => {
  const inputSetupWizardSteps = useContext(InputSetupWizardStepsContext) as InputSetupWizardStepsContextType<StepsData>;

  if (!inputSetupWizardSteps) {
    throw new Error('useInputSetupWizardSteps hook needs to be used inside InputSetupWizardContextSteps.Provider');
  }

  return inputSetupWizardSteps;
};

export default useInputSetupWizardSteps;
