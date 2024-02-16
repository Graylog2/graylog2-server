import React from 'react';

import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';

const ReplaceCluster = ({ nextSteps, onTriggerStep }: MigrationStepComponentProps) => (
  <>
    Replace Cluster
    <MigrationStepTriggerButtonToolbar nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
  </>

);
export default ReplaceCluster;
