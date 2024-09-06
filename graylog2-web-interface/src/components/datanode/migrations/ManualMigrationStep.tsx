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
import InPlaceMigration from 'components/datanode/migrations/InPlaceMigration';
import useMigrationState from 'components/datanode/hooks/useMigrationState';
import type { MigrationActions, StepArgs } from 'components/datanode/Types';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import {
  IN_PLACE_MIGRATION_STEPS,
  MIGRATION_STATE,
  REMOTE_REINDEXING_MIGRATION_STEPS,
} from 'components/datanode/Constants';
import RemoteReindexingMigration from 'components/datanode/migrations/RemoteReindexingMigration';
import MigrationError from 'components/datanode/migrations/common/MigrationError';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const ManualMigrationStep = () => {
  const { currentStep } = useMigrationState();
  const { onTriggerNextState } = useTriggerMigrationState();
  const sendTelemetry = useSendTelemetry();

  const onMigrationStepChange = async (step: MigrationActions, args?: StepArgs = {}) => onTriggerNextState({ step, args });

  const handleSelectMigrationType = async (step: MigrationActions, args?: StepArgs = {}) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.MIGRATION_TYPE_SELECTED, {
      app_pathname: 'datanode',
      app_section: 'migration',
      event_details: { migration_type: (step === 'SELECT_ROLLING_UPGRADE_MIGRATION') ? 'IN-PLACE' : 'REMOTE REINDEX' },
    });

    return onTriggerNextState({ step, args });
  };

  const migrationTypeOptions = [
    { label: 'In-Place migration', value: 'SELECT_ROLLING_UPGRADE_MIGRATION' },
    { label: 'Remote Re-indexing Migration', value: 'SELECT_REMOTE_REINDEX_MIGRATION' },
  ].filter((path) => currentStep.next_steps.includes(path.value));

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
                      onChange={handleSelectMigrationType}
                      value={null} />
            </Input>
          </form>
        </Col>
      )}
      <MigrationError errorMessage={currentStep.error_message} />
      {(currentStep && IN_PLACE_MIGRATION_STEPS.includes(currentStep.state)) && <InPlaceMigration onTriggerStep={onMigrationStepChange} currentStep={currentStep} />}
      {(currentStep && REMOTE_REINDEXING_MIGRATION_STEPS.includes(currentStep.state)) && <RemoteReindexingMigration onTriggerStep={onMigrationStepChange} currentStep={currentStep} />}
    </>
  );
};

export default ManualMigrationStep;
