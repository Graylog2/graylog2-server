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
import { useMemo } from 'react';

import { MIGRATION_STATE, MIGRATION_WIZARD_STEPS } from 'components/datanode/Constants';
import type { MigrationState } from 'components/datanode/Types';
import useMigrationState from 'components/datanode/hooks/useMigrationState';

const migrationStep = (
  currentStep: MigrationState,
  isLoading: boolean,
) => {
  // @ts-ignore
  if (!isLoading && MIGRATION_WIZARD_STEPS.includes(currentStep?.state)) {
    return currentStep;
  }

  return { state: MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key, next_steps: ['SELECT_ROLLING_UPGRADE_MIGRATION', 'SELECT_REMOTE_REINDEX_MIGRATION'] } as MigrationState;
};

const useMigrationWizardStep = () => {
  const { currentStep, isLoading } = useMigrationState();
  const step = migrationStep(currentStep, isLoading);

  return useMemo(() => ({
    isLoading: isLoading,
    step,
    errors: null,
  }), [isLoading, step]);
};

export default useMigrationWizardStep;
