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
import { useState } from 'react';

import { CertificateRenewal } from '@graylog/server-api';

import type { DataNode } from 'components/datanode/Types';
import UserNotification from 'util/UserNotification';
import { Button } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const renewalWording = {
  buttonTitle: 'Renew certificate',
  buttonLoadingTitle: 'Renewing certificate',
  successActionTitle: 'renewed',
  errorActionTitle: 'renewal',
  telemetryAppSection: 'renewing-certificate',
  buttonStyle: 'primary',
} as const;

const provisioningWording = {
  buttonTitle: 'Provision certificate',
  buttonLoadingTitle: 'Provisioning certificate...',
  successActionTitle: 'provisioned',
  errorActionTitle: 'provisioning',
  telemetryAppSection: 'provisioning-certificate',
  buttonStyle: 'success',
} as const;

export const CertRenewalButton = ({ nodeId, status }: { nodeId: string; status: DataNode['status'] }) => {
  const sendTelemetry = useSendTelemetry('data-node');
  const [isRenewing, setIsRenewing] = useState(false);
  const { buttonTitle, buttonLoadingTitle, successActionTitle, errorActionTitle, telemetryAppSection, buttonStyle } =
    status === 'UNCONFIGURED' ? provisioningWording : renewalWording;

  const onCertificateRenewal = () => {
    setIsRenewing(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.CERTIFICATE_RENEWAL_UPDATED, {
      app_action_value: telemetryAppSection,
    });

    CertificateRenewal.initiateCertRenewalForNode(nodeId)
      .then(() => {
        UserNotification.success(`Certificate ${successActionTitle} successfully`);
      })
      .catch((error) => {
        UserNotification.error(`Certificate ${errorActionTitle} failed with error: ${error}`);
      })
      .finally(() => {
        setIsRenewing(false);
      });
  };

  return (
    <Button onClick={onCertificateRenewal} bsSize="xsmall" bsStyle={buttonStyle}>
      {isRenewing ? buttonLoadingTitle : buttonTitle}
    </Button>
  );
};
