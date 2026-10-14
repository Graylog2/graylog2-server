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

import { Alert, Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';

import useConfigurationOverrides from './useConfigurationOverrides';
import DataNodeConfigurationForm from './DataNodeConfigurationForm';
import type { ConfigurationGroup } from './types';

type Props = { group: ConfigurationGroup; nodeId?: string | null };

const DataNodeConfiguration = ({ group, nodeId = null }: Props) => {
  const { data, isLoading, error } = useConfigurationOverrides(nodeId);
  const globalConfiguration = useConfigurationOverrides(null, nodeId !== null);

  if (isLoading || (nodeId !== null && globalConfiguration.isLoading))
    return <Spinner text="Loading Data Node configuration..." />;
  if (error || (nodeId !== null && globalConfiguration.error))
    return <Alert bsStyle="danger">Could not load Data Node configuration. Please try again.</Alert>;
  if (!data) return null;

  return (
    <Row>
      <Col md={8}>
        <DataNodeConfigurationForm
          key={`${nodeId ?? 'global'}:${group.id}`}
          configuration={data}
          group={group}
          inheritedOverrides={nodeId === null ? undefined : globalConfiguration.data?.overrides}
        />
      </Col>
    </Row>
  );
};

export default DataNodeConfiguration;
