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
import styled from 'styled-components';

import { Alert } from 'components/bootstrap';
import useCompatibilityCheck from 'components/datanode/hooks/useCompatibilityCheck';
import { Spinner } from 'components/common';
import CompatibilityStatus from 'components/datanode/migrations/CompatibilityStatus';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';

const CompatibilityAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const CompatibilityCheckStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { error: requestError, data, isInitialLoading, isError } = useCompatibilityCheck();

  if (isInitialLoading) {
    return <Spinner text="Loading compatibility check results..." />;
  }

  const errors = Object.values(data || {}).flatMap((value) => (value?.compatibility_errors || []));
  const warnings = Object.values(data || {}).flatMap((value) => (value?.compatibility_warnings || []));
  const isCompatible = errors.length === 0;

  return (
    <>
      <h3>Directory compatibility check</h3>
      {isCompatible && !warnings.length && (
        <CompatibilityAlert bsStyle="success">
          <h4>Your existing OpenSearch data can be migrated to Data Node.</h4>
        </CompatibilityAlert>
      )}
      {(!isCompatible || isError) && (
        <CompatibilityAlert bsStyle="danger">
          {!isError && !isCompatible && (
            <>
              <h4>Your existing OpenSearch data cannot be migrated to Data Node.</h4>
              <br />
              {errors.map((error) => <dd key={error}>{error}</dd>)}
            </>
          )}
          {isError && (
            <>
              <h4>There was an error checking the compatibility</h4>
              <p>{requestError.message}</p>
            </>
          )}
        </CompatibilityAlert>
      )}
      {warnings.length > 0 && (
        <CompatibilityAlert bsStyle="warning">
          {warnings.map((warning) => <dd key={warning}>{warning}</dd>)}
        </CompatibilityAlert>
      )}
      <br />
      {!isCompatible && (<p>Your OpenSearch cluster cannot be migrated to this Data Node version because it&apos;s not compatible.</p>)}
      {isCompatible && data && Object.keys(data).map((hostname) => (
        <CompatibilityStatus key={hostname} hostname={hostname} opensearchVersion={data[hostname].opensearch_version} nodeInfo={data[hostname].info} />
      ))}
      <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default CompatibilityCheckStep;
