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
import { useState } from 'react';

import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
import { Wizard } from 'components/common';
import MigrationHelpStep from 'components/datanode/migrations/MigrationHelpStep';

const STEP_KEYS = ['welcome', 'compatibility-check'];

const MigrationWizard = () => {
  const [activeStep, setActiveStep] = useState(STEP_KEYS[1]);

  const onWizardStepChange = (step: string) => {
    setActiveStep(step);
  };

  const steps = [
    {
      key: 'welcome',
      title: (<>Welcome</>),
      component: <MigrationHelpStep onStepComplete={() => onWizardStepChange(STEP_KEYS[1])} />,
    },
    {
      key: 'compatibility-check',
      title: (<>Compatibitlity Check</>),
      component: <CompatibilityCheckStep onStepComplete={() => onWizardStepChange(STEP_KEYS[2])} />,
    },
  ];

  return (
    <Wizard steps={steps}
            activeStep={activeStep}
            onStepChange={onWizardStepChange}
            horizontal
            justified
            containerClassName=""
            hidePreviousNextButtons />
  );
};

export default MigrationWizard;
