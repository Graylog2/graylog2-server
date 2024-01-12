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
