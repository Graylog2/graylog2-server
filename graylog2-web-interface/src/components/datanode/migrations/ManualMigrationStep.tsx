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
import React from 'react';

import { Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import MigrateExistingCluster from 'components/datanode/migrations/MigrateExistingCluster';
import useMigrationState from 'components/datanode/hooks/useMigrationState';
import type { MigrationActions } from 'components/datanode/Types';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import { MIGRATION_STATE, ROLLING_UPGRADE_MIGRATION_STEPS } from 'components/datanode/Constants';
import RemoteReindexingMigration from 'components/datanode/migrations/RemoteReindexingMigration';

type MigrationTypeSteps = Extract<MigrationActions, 'SELECT_ROLLING_UPGRADE_MIGRATION'|'SELECT_REMOTE_REINDEX_MIGRATION'>

const ManualMigrationStep = () => {
  const migrationTypeOptions = [{ label: 'Rolling upgrade migration', value: 'SELECT_ROLLING_UPGRADE_MIGRATION' }, { label: 'Remote Re-indexing Migration', value: 'SELECT_REMOTE_REINDEX_MIGRATION' }];
  const { currentStep } = useMigrationState();
  const { onTriggerNextState } = useTriggerMigrationState();

  const onMigrationStepchange = async (type: MigrationTypeSteps) => {
    await onTriggerNextState({ step: type });
  };

  return (
    <>
      {currentStep?.state === MIGRATION_STATE.NEW.key && (
        <Col>
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
                      options={migrationTypeOptions}
                      matchProp="label"
                      onChange={onMigrationStepchange}
                      value={null} />
            </Input>
          </form>
        </Col>
      )}
      {(currentStep && ROLLING_UPGRADE_MIGRATION_STEPS.includes(currentStep.state)) && <MigrateExistingCluster onTriggerNextStep={onTriggerNextState} currentStep={currentStep} />}
      {(currentStep?.state === 'REMOTE_REINDEX_WELCOME') && <RemoteReindexingMigration />}
    </>
  );
};

export default ManualMigrationStep;
