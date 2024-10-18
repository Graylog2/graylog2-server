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
import styled, { css } from 'styled-components';

import type { MigrationActions, MigrationStateItem, OnTriggerStepFunction, StepArgs } from 'components/datanode/Types';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { MIGRATION_ACTIONS } from 'components/datanode/Constants';
import useMigrationState from 'components/datanode/hooks/useMigrationState';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';

const StyledButtonToolbar = styled(ButtonToolbar)(({ theme }) => css`
  margin-top: ${theme.spacings.md};
  flex-wrap: wrap;
`);

const getSortedNextSteps = (nextSteps: MigrationActions[]) => nextSteps.reduce((sortedNextSteps, step) => {
  if (!MIGRATION_ACTIONS[step]?.label) {
    return [step, ...sortedNextSteps];
  }

  sortedNextSteps.push(step);

  return sortedNextSteps;
}, []);

type Props = {
    nextSteps?: Array<MigrationActions>,
    disabled?: boolean,
    onTriggerStep: OnTriggerStepFunction,
    args?: StepArgs,
    hidden?: boolean,
    children?: React.ReactNode,
}

const getTelemetryEvent = (state: MigrationStateItem, step: MigrationActions): TelemetryEventType => {
  switch (state) {
    case 'MIGRATION_WELCOME_PAGE':
      if (step === 'SHOW_MIGRATION_SELECTION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.WELCOME_GO_TO_MIGRATION_STEPS_CLICKED;
      if (step === 'SHOW_RENEWAL_POLICY_CREATION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.WELCOME_CONFIGURE_CERTIFICATE_RENEWAL_POLICY_CLICKED;

      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.WELCOME_NEXT_CLICKED;
    case 'CA_CREATION_PAGE':
      if (step === 'SHOW_MIGRATION_SELECTION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_GO_TO_MIGRATION_STEPS_CLICKED;
      if (step === 'SHOW_RENEWAL_POLICY_CREATION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_CONFIGURE_CERTIFICATE_RENEWAL_POLICY_CLICKED;

      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_NEXT_CLICKED;
    case 'RENEWAL_POLICY_CREATION_PAGE':
      if (step === 'SHOW_MIGRATION_SELECTION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_GO_TO_MIGRATION_STEPS_CLICKED;

      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_NEXT_CLICKED;
    case 'ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.INPLACE_RUN_DIRECTORY_COMPATIBILITY_CHECK_CLICKED;
    case 'DIRECTORY_COMPATIBILITY_CHECK_PAGE':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.INPLACE_DIRECTORY_COMPATIBILITY_CHECK_NEXT_CLICKED;
    case 'JOURNAL_SIZE_DOWNTIME_WARNING':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.INPLACE_JOURNAL_SIZE_DOWNTIME_WARNING_NEXT_CLICKED;
    case 'MESSAGE_PROCESSING_STOP':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.INPLACE_STOP_MESSAGE_PROCESSING_NEXT_CLICKED;
    case 'RESTART_GRAYLOG':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.INPLACE_RESTART_GRAYLOG_NEXT_CLICKED;
    case 'REMOTE_REINDEX_WELCOME_PAGE':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_WELCOME_NEXT_CLICKED;
    case 'EXISTING_DATA_MIGRATION_QUESTION_PAGE':
      if (step === 'SKIP_EXISTING_DATA_MIGRATION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_MIGRATE_EXISTING_DATA_QUESTION_SKIP_CLICKED;

      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_MIGRATE_EXISTING_DATA_QUESTION_NEXT_CLICKED;
    case 'MIGRATE_EXISTING_DATA':
      if (step === 'START_REMOTE_REINDEX_MIGRATION') return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_MIGRATE_EXISTING_DATA_START_CLICKED;

      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_MIGRATE_EXISTING_DATA_CHECK_CONNECTION_CLICKED;
    case 'ASK_TO_SHUTDOWN_OLD_CLUSTER':
      return TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_SHUTDOWN_OLD_CLUSTER_NEXT_CLICKED;
    default:
      return null;
  }
};

const MigrationStepTriggerButtonToolbar = ({ nextSteps = [], disabled = false, onTriggerStep, args = {}, hidden = false, children }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { currentStep } = useMigrationState();

  if (hidden) {
    return null;
  }

  const handleButtonClick = (step: MigrationActions) => {
    const eventType = getTelemetryEvent(currentStep?.state, step);

    if (eventType) {
      sendTelemetry(eventType, {
        app_pathname: 'datanode',
        app_section: 'migration',
      });
    }

    onTriggerStep(step, args);
  };

  return (
    <StyledButtonToolbar>
      {getSortedNextSteps(nextSteps).map((step, index) => <Button key={step} bsStyle={index ? 'default' : 'success'} bsSize="small" disabled={disabled} onClick={() => handleButtonClick(step)}>{MIGRATION_ACTIONS[step]?.label || 'Next'}</Button>)}
      {children}
    </StyledButtonToolbar>
  );
};

export default MigrationStepTriggerButtonToolbar;
