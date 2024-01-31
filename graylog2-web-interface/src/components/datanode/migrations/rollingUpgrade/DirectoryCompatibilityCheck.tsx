import React from 'react';
import styled from 'styled-components';

import type { MigrationActions } from 'components/datanode/Types';
import { Alert, Button } from 'components/bootstrap';

type Props = {
  onStepComplete: (step: {step: MigrationActions}) => void,
  nextSteps: Array<MigrationActions>
};
const CompatibilityAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const DirectoryCompatibilityCheck = ({ onStepComplete, nextSteps }: Props) => (
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
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
  </>
);
export default DirectoryCompatibilityCheck;
