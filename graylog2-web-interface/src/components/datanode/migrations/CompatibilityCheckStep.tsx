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
import styled from 'styled-components';

import useCompatibilityCheck from 'components/datanode/hooks/useCompatibilityCheck';
import { Spinner } from 'components/common';
import { Alert, Button } from 'components/bootstrap';
import CompatibilityStatus from 'components/datanode/migrations/CompatibilityStatus';

type Props = {
  onStepComplete: () => void,
};
const CompatibilityAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const CompatibilityCheckStep = ({ onStepComplete }: Props) => {
  const { error: requestError, data, isInitialLoading, isError } = useCompatibilityCheck();

  if (isInitialLoading) {
    return <Spinner text="Loading compatibility check results..." />;
  }

  const isCompatible = data?.compatibility_errors.length === 0;

  return (
    <>
      <CompatibilityAlert bsStyle={(!isError && isCompatible) ? 'success' : 'danger'}>
        {isCompatible && <h4>Your existing opensearch data can be migrated to Datanode.</h4>}
        {!isError && !isCompatible && (
          <>
            <h4>Your existing opensearch data cannot be migrated to Datanode.</h4>
            <br />
            Error: {data?.compatibility_errors.map((error) => <dd key={error}>{error}</dd>)}
          </>
        )}
        {isError && (
          <>
            <h4>There was an error checking the compatibility</h4>
            <p>{requestError.message}</p>
          </>
        )}
      </CompatibilityAlert>
      {isCompatible && <CompatibilityStatus opensearchVersion={data.opensearch_version} nodeInfo={data.info} />}
      <Button bsStyle="success" onClick={() => onStepComplete()}>
        Next
      </Button>
    </>
  );
};

export default CompatibilityCheckStep;
