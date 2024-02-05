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

import { Alert, Button } from 'components/bootstrap';

type Props = {
  onStepComplete: () => void,
};
const CompatibilityAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const DirectoryCompatibilityCheck = ({ onStepComplete }: Props) => (
  <>
    <CompatibilityAlert bsStyle={true ? 'success' : 'danger'}>
      {true && <h4>Your existing opensearch data can be migrated to Datanode.</h4>}
      {!true && (
      <>
        <h4>Your existing opensearch data cannot be migrated to Datanode.</h4>
        <br />
      </>
      )}
    </CompatibilityAlert>
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete()}>Next</Button>
  </>
);
export default DirectoryCompatibilityCheck;
