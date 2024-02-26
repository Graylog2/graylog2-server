// @ts-nocheck
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

import { Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import RollingUpgradeMigration from 'components/datanode/migrations/RollingUpgradeMigration';
import useMigrationState from 'components/datanode/hooks/useMigrationState';
import type { MigrationActions, StepArgs } from 'components/datanode/Types';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import {
  MIGRATION_STATE,
  REMOTE_REINDEXING_MIGRATION_STEPS,
  ROLLING_UPGRADE_MIGRATION_STEPS,
} from 'components/datanode/Constants';
import RemoteReindexingMigration from 'components/datanode/migrations/RemoteReindexingMigration';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

const ManualMigrationStep = () => {
  const migrationTypeOptions = [{ label: 'Rolling upgrade migration', value: 'SELECT_ROLLING_UPGRADE_MIGRATION' }, { label: 'Remote Re-indexing Migration', value: 'SELECT_REMOTE_REINDEX_MIGRATION' }];
  const { currentStep } = useMigrationState();
  const { onTriggerNextState } = useTriggerMigrationState();

  const onMigrationStepChange = (step: MigrationActions, args?: StepArgs = {}) => {
    onTriggerNextState({ step, args });
  };

  return (
    <>
      {currentStep?.state === MIGRATION_STATE.MIGRATION_SELECTION_PAGE.key && (
        <Col md={8} data-testid="datanode-migration-select">
          <form className="form form-horizontal" onSubmit={() => {}}>
            <Input id="datanode-migration-type-select"
                   label="Migration type"
                   required
                   autoFocus
                   help="The type of migration you want to do."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select placeholder="Select migration type"
                      clearable={false}
                      inputId="datanode-migration-type-select"
                      options={migrationTypeOptions}
                      matchProp="label"
                      onChange={onMigrationStepChange}
                      value={null} />
            </Input>
          </form>
        </Col>
      )}
      <MigrationError errorMessage={currentStep.error_message} />
      {(currentStep && ROLLING_UPGRADE_MIGRATION_STEPS.includes(currentStep.state)) && <RollingUpgradeMigration onTriggerNextStep={onMigrationStepChange} currentStep={currentStep} />}
      {(currentStep && REMOTE_REINDEXING_MIGRATION_STEPS.includes(currentStep.state)) && <RemoteReindexingMigration onTriggerNextStep={onMigrationStepChange} currentStep={currentStep} />}
    </>
  );
};

export default ManualMigrationStep;
