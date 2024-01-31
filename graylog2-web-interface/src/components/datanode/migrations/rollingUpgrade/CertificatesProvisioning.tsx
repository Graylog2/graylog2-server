import React from 'react';

import type { MigrationActions } from 'components/datanode/Types';
import { Button } from 'components/bootstrap';
import CertificateProvisioning from 'components/datanode/DataNodeConfiguration/CertificateProvisioning';

type Props = {
  onStepComplete: (step: {step: MigrationActions}) => void,
  nextSteps: Array<MigrationActions>
};

const CertificatesProvisioning = ({ onStepComplete, nextSteps }: Props) => (
  <>
    <CertificateProvisioning onSkipProvisioning={() => {}} />
    <br />
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
  </>
);
export default CertificatesProvisioning;
