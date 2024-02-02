import React from 'react';

import { Button } from 'components/bootstrap';
import CertificateRenewalPolicyConfig from 'components/datanode/DataNodeConfiguration/CertificateRenewalPolicyConfig';

type Props = {
  onStepComplete: () => void,
};
const CertificateRenewalStep = ({ onStepComplete }: Props) => (
  <>
    <CertificateRenewalPolicyConfig />
    <Button bsStyle="success" onClick={() => onStepComplete()}>
      Next
    </Button>
  </>

);
export default CertificateRenewalStep;
