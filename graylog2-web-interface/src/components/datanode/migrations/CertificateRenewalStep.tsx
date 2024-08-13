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

import CertificateRenewalPolicyConfig from 'components/datanode/DataNodeConfiguration/CertificateRenewalPolicyConfig';
import { Space } from 'preflight/components/common';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

const CertificateRenewalStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => (
  <>
    <MigrationError errorMessage={currentStep.error_message} />
    <CertificateRenewalPolicyConfig />
    <Space h="xs" />
    {(currentStep.next_steps.length <= 0) && (<p>Please create a certificate renewal policy before proceeding.</p>)}
    <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
  </>
);
export default CertificateRenewalStep;
