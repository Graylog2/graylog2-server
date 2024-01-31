import React from 'react';

import type { MigrationActions } from 'components/datanode/Types';
import { Button } from 'components/bootstrap';

type Props = {
  onStepComplete: (step: {step: MigrationActions}) => void,
  nextSteps: Array<MigrationActions>
};

const MigrateActions = ({ onStepComplete, nextSteps }: Props) => (
  <>
    Migrations Steps
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
  </>

);
export default MigrateActions;
