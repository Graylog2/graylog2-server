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
  const { isError, data } = useCompatibilityCheck();

  if (isError) {
    return <Spinner text="Loading..." />;
  }

  const isCompatible = data?.compatibility_errors.length === 0;

  return (
    <>
      <CompatibilityAlert bsStyle={isCompatible ? 'success' : 'danger'}>
        {isCompatible && <h4>Your existing opensearch data can be migrated to Datanode.</h4>}
        {!isCompatible && (
          <>
            <h4>Your existing opensearch data cannot be migrated to Datanode.</h4>
            <br />
            Error: {data?.compatibility_errors.map((error) => <dd>{error}</dd>)}
          </>
        )}
      </CompatibilityAlert>
      {isCompatible && <CompatibilityStatus opensearchVersion={data.opensearch_version} nodeInfo={data.info} />}
      <Button bsStyle="success" onClick={() => onStepComplete()} disabled={!isCompatible}>
        Start Migration
      </Button>
    </>
  );
};

export default CompatibilityCheckStep;
