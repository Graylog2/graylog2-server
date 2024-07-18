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

import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import { MIGRATION_STATE } from 'components/datanode/Constants';
import { Spinner } from 'components/common';
import useMigrationState from 'components/datanode/hooks/useMigrationState';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import { Alert } from 'components/bootstrap';

const CertificatesProvisioning = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { currentStep: step, isLoading } = useMigrationState(3000);

  if (isLoading) {
    return <Spinner text="Loading migration state." />;
  }

  const isProvisioningOverview = (step.state === MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key) || (step.state === MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_PAGE.key);
  const isProvisioningRunning = (step.state === MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key) || (step.state === MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_RUNNING.key);
  const haveNextStep = step?.next_steps?.length > 0;

  return (
    <>
      {isProvisioningOverview && (
      <p>
        Certificate authority has been configured successfully.<br />
        You can now provision certificate for your Data Nodes.
      </p>
      )}
      {(isProvisioningRunning && !haveNextStep) && (
      <Spinner text="Provisioning certificate" />
      )}
      {(isProvisioningRunning && haveNextStep) && (
      <Alert bsStyle="success">Provisioning the Data Node finished.</Alert>
      )}
      <MigrationDatanodeList />
      <br />
      <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default CertificatesProvisioning;
