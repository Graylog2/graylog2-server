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

import { Spinner, Wizard } from 'components/common';
import CAStep from 'components/datanode/migrations/CAStep';
import ManualMigrationStep from 'components/datanode/migrations/ManualMigrationStep';
import { MIGRATION_STATE } from 'components/datanode/Constants';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import type { MigrationActions, StepArgs } from 'components/datanode/Types';
import useMigrationWizardStep from 'components/datanode/hooks/useMigrationWizardStep';
import MigrationWelcomeStep from 'components/datanode/migrations/MigrationWelcomeStep';
import CertificateRenewalStep from 'components/datanode/migrations/CertificateRenewalStep';
import MigrationFinishedStep from 'components/datanode/migrations/MigrationFinishedStep';

const MigrationWizard = () => {
  const { step: currentStep, isLoading } = useMigrationWizardStep();
  const { onTriggerNextState } = useTriggerMigrationState();

  if (isLoading) {
    return <Spinner text="Loading ..." />;
  }

  const onTriggerStep = async (step: MigrationActions, args: StepArgs = {}) => onTriggerNextState({ step, args });

  const { state: activeStep } = currentStep;

  const steps = [
    {
      key: MIGRATION_STATE.MIGRATION_WELCOME_PAGE.key,
      title: MIGRATION_STATE.MIGRATION_WELCOME_PAGE.description,
      component: <MigrationWelcomeStep currentStep={currentStep} onTriggerStep={onTriggerStep} />,
    },
    {
      key: MIGRATION_STATE.CA_CREATION_PAGE.key,
      title: MIGRATION_STATE.CA_CREATION_PAGE.description,
      component: <CAStep currentStep={currentStep} onTriggerStep={onTriggerStep} />,
    },
    {
      key: MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.key,
      title: MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.description,
      component: <CertificateRenewalStep currentStep={currentStep} onTriggerStep={onTriggerStep} />,
    },
    {
      key: MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key,
      title: MIGRATION_STATE.MIGRATION_SELECTION_PAGE.description,
      component: <ManualMigrationStep />,
    },
    {
      key: MIGRATION_STATE.FINISHED.key,
      title: MIGRATION_STATE.FINISHED.description,
      component: <MigrationFinishedStep />,
    },
  ];

  return (
    <Wizard steps={steps}
            activeStep={activeStep}
            onStepChange={() => {}}
            horizontal
            justified
            containerClassName="migration-wizard"
            hidePreviousNextButtons />
  );
};

export default MigrationWizard;
