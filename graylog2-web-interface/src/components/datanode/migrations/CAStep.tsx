import React from 'react';

import CAConfiguration from 'components/datanode/DataNodeConfiguration/CAConfiguration';
import useMigrationStep from 'components/datanode/hooks/useMigrationStep';
import { MIGRATION_STEP } from 'components/datanode/Constants';
import CertificateRenewalPolicyConfig from 'components/datanode/DataNodeConfiguration/CertificateRenewalPolicyConfig';
import { Button } from 'components/bootstrap';

type Props = {
  onStepComplete: () => void,
};

const CaStep = ({ onStepComplete }: Props) => {
  const { step } = useMigrationStep();

  return (
    <>
      {step === MIGRATION_STEP.CA_CONFIGURATION.key && <CAConfiguration />}
      {step === MIGRATION_STEP.RENEWAL_POLICY_CONFIGURATION.key && <CertificateRenewalPolicyConfig />}
      <br />
      <Button bsStyle="success" onClick={() => onStepComplete()}>
        Next
      </Button>
    </>
  );
};

export default CaStep;
