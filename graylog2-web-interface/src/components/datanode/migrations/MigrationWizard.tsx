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

import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
import { Spinner, Wizard } from 'components/common';
import CAStep from 'components/datanode/migrations/CAStep';
import ManualMigrationStep from 'components/datanode/migrations/ManualMigrationStep';
import { MIGRATION_STATE } from 'components/datanode/Constants';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import type { MigrationActions } from 'components/datanode/Types';
import useMigrationWizardStep from 'components/datanode/hooks/useMigrationWizardStep';
import MigrationWelcomeStep from 'components/datanode/migrations/MigrationWelcomeStep';
import CertificateRenewalStep from 'components/datanode/migrations/CertificateRenewalStep';
import ShutdownClusterStep from 'components/datanode/migrations/ShutdownClusterStep';
import MigrationFinishedStep from 'components/datanode/migrations/MigrationFinishedStep';
import ConnectionStringRemovalStep from 'components/datanode/migrations/ConnectionStringRemovalStep';

const MigrationWizard = () => {
  const { step: currentStep, isLoading } = useMigrationWizardStep();
  const { onTriggerNextState } = useTriggerMigrationState();

  if (isLoading) {
    return <Spinner text="Loading ..." />;
  }

  const onWizardStepChange = (step: MigrationActions) => {
    onTriggerNextState({ step, args: {} });
  };

  const { state: activeStep, next_steps: nextSteps } = currentStep;

  const steps = [
    {
      key: MIGRATION_STATE.MIGRATION_WELCOME_PAGE.key,
      title: MIGRATION_STATE.MIGRATION_WELCOME_PAGE.description,
      component: <MigrationWelcomeStep onStepComplete={() => onWizardStepChange(nextSteps[0])}
                                       onSkipCompatibilityCheck={() => onWizardStepChange(nextSteps[1])} />,
    },
    {
      key: MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key,
      title: MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.description,
      component: <CompatibilityCheckStep onStepComplete={() => onWizardStepChange(nextSteps[0])} />,
    },
    {
      key: MIGRATION_STATE.CA_CREATION_PAGE.key,
      title: MIGRATION_STATE.CA_CREATION_PAGE.description,
      component: <CAStep onStepComplete={() => onWizardStepChange(nextSteps[0])} />,
    },
    {
      key: MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.key,
      title: MIGRATION_STATE.RENEWAL_POLICY_CREATION_PAGE.description,
      component: <CertificateRenewalStep onStepComplete={() => onWizardStepChange(nextSteps[0])} />,
    },
    {
      key: MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key,
      title: MIGRATION_STATE.MIGRATION_SELECTION_PAGE.description,
      component: <ManualMigrationStep />,
    },
    {
      key: MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key,
      title: MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.description,
      component: <ShutdownClusterStep onStepComplete={() => onWizardStepChange(nextSteps[0])} />,
    },
    {
      key: MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key,
      title: MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.description,
      component: <ConnectionStringRemovalStep onStepComplete={() => onWizardStepChange(nextSteps[0])} />,
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
            containerClassName=""
            hidePreviousNextButtons />
  );
};

export default MigrationWizard;
