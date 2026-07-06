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
import { Icon } from 'components/common';

const OpenSearchStatusLine = ({
  currentOpenSearchVersion,
  isOpenSearchVersionError,
  isLoadingOpenSearchVersion,
  isOpenSearchUpToDate,
  unavailableDataNodeCount,
}: {
  currentOpenSearchVersion: string | undefined;
  isOpenSearchVersionError: boolean;
  isLoadingOpenSearchVersion: boolean;
  isOpenSearchUpToDate: boolean;
  unavailableDataNodeCount: number;
}) => {
  if (isLoadingOpenSearchVersion) {
    return <p>Checking OpenSearch version...</p>;
  }

  if (isOpenSearchVersionError) {
    return (
      <p>
        <Icon name="warning" bsStyle="warning" /> Could not check Data Nodes&apos; embedded OpenSearch version.
      </p>
    );
  }

  // A node that is down or still starting may come back with a different OpenSearch version than its
  // metadata claims, so neither "up to date" nor "not up to date" can be stated honestly.
  if (unavailableDataNodeCount > 0) {
    return (
      <p>
        <Icon name="warning" bsStyle="warning" /> Data Nodes&apos; embedded OpenSearch state cannot be confirmed
        while {unavailableDataNodeCount} Data {unavailableDataNodeCount === 1 ? 'Node is' : 'Nodes are'} unavailable.
      </p>
    );
  }

  if (isOpenSearchUpToDate) {
    return (
      <p>
        <Icon name="check_circle" bsStyle="success" /> Data Nodes&apos; embedded OpenSearch is up to date
        {currentOpenSearchVersion ? <b>{` (${currentOpenSearchVersion}).`}</b> : '.'}
      </p>
    );
  }

  return (
    <p>
      <Icon name="warning" bsStyle="warning" /> Data Nodes&apos; embedded OpenSearch is not up to date.
    </p>
  );
};

const UpgradeStatusAlert = ({
  currentOpenSearchVersion,
  isOpenSearchVersionError,
  isOpenSearchUpToDate,
  isLoadingOpenSearchVersion,
  unavailableDataNodeCount,
}: {
  currentOpenSearchVersion: string | undefined;
  isOpenSearchVersionError: boolean;
  isOpenSearchUpToDate: boolean;
  isLoadingOpenSearchVersion: boolean;
  unavailableDataNodeCount: number;
}) => (
  <Alert bsStyle={isOpenSearchUpToDate ? 'success' : 'warning'} noIcon>
    <p>
      <Icon name="check_circle" bsStyle="success" /> All your Data Nodes are up to date.
    </p>
    <OpenSearchStatusLine
      currentOpenSearchVersion={currentOpenSearchVersion}
      isOpenSearchVersionError={isOpenSearchVersionError}
      isLoadingOpenSearchVersion={isLoadingOpenSearchVersion}
      isOpenSearchUpToDate={isOpenSearchUpToDate}
      unavailableDataNodeCount={unavailableDataNodeCount}
    />
  </Alert>
);

export default UpgradeStatusAlert;
