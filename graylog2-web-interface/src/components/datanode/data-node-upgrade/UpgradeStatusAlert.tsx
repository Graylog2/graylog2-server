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

import { Alert } from 'components/bootstrap';
import { Icon, Spinner } from 'components/common';
import type { OpenSearchUpgradeStatus } from 'components/datanode/opensearch-upgrade/hooks/useOpenSearchUpgradeStatus';
import assertUnreachable from 'logic/assertUnreachable';

type Props = {
  currentOpenSearchVersion: string | undefined;
  status: OpenSearchUpgradeStatus;
  unavailableDataNodeCount: number;
};

const ALERT_STYLE: Record<OpenSearchUpgradeStatus, 'info' | 'success' | 'warning'> = {
  upgrading: 'info',
  checking: 'info',
  error: 'warning',
  outdated: 'warning',
  unconfirmed: 'warning',
  'up-to-date': 'success',
};

const OpenSearchStatusLine = ({ currentOpenSearchVersion, status, unavailableDataNodeCount }: Props) => {
  switch (status) {
    case 'checking':
      return (
        <p>
          <Spinner text="Checking OpenSearch status..." />
        </p>
      );
    case 'upgrading':
      return (
        <p>
          <Icon name="info" /> OpenSearch rolling upgrade is in progress.
        </p>
      );
    case 'error':
      return (
        <p>
          <Icon name="warning" bsStyle="warning" /> Could not check Data Nodes&apos; embedded OpenSearch version.
        </p>
      );
    case 'unconfirmed':
      return (
        <p>
          <Icon name="warning" bsStyle="warning" /> Data Nodes&apos; embedded OpenSearch state cannot be confirmed
          while {unavailableDataNodeCount} Data {unavailableDataNodeCount === 1 ? 'Node is' : 'Nodes are'} unavailable.
        </p>
      );
    case 'up-to-date':
      return (
        <p>
          <Icon name="check_circle" bsStyle="success" /> Data Nodes&apos; embedded OpenSearch is up to date
          {currentOpenSearchVersion ? <b>{` (${currentOpenSearchVersion}).`}</b> : '.'}
        </p>
      );
    case 'outdated':
      return (
        <p>
          <Icon name="warning" bsStyle="warning" /> Data Nodes&apos; embedded OpenSearch is not up to date.
        </p>
      );
    default:
      return assertUnreachable(status, 'Unknown OpenSearch upgrade status');
  }
};

const UpgradeStatusAlert = ({ currentOpenSearchVersion, status, unavailableDataNodeCount }: Props) => (
  <Alert bsStyle={ALERT_STYLE[status]} noIcon>
    <p>
      <Icon name="check_circle" bsStyle="success" /> All your Data Nodes are up to date.
    </p>
    <OpenSearchStatusLine
      currentOpenSearchVersion={currentOpenSearchVersion}
      status={status}
      unavailableDataNodeCount={unavailableDataNodeCount}
    />
  </Alert>
);

export default UpgradeStatusAlert;
