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
import { useMemo } from 'react';
import { useQueries, useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';

import { SystemInputs } from '@graylog/server-api';

import { Button, Alert, Row, Col, Table } from 'components/bootstrap';
import { Link } from 'components/common';
import Routes from 'routing/Routes';
import InputStateBadge from 'components/inputs/InputStateBadge';
import useCurrentUser from 'hooks/useCurrentUser';
import useInputMutations from 'hooks/useInputMutations';
import { isPermitted } from 'util/PermissionsMixin';
import useInputsStates from 'hooks/useInputsStates';

import { useCollectorInputIds } from '../hooks';
import { COLLECTOR_INPUT_IDS_KEY_PREFIX } from '../hooks/useCollectorInputIds';

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

type Props = {
  defaultPort: number;
  isInitialSetup: boolean;
};

const IngestEndpointStatus = ({ defaultPort, isInitialSetup }: Props) => {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const { createInput } = useInputMutations();
  const { data: collectorInputIds = [], isLoading } = useCollectorInputIds();
  const { data: inputStates, isLoading: isLoadingInputStates } = useInputsStates({ enabled: collectorInputIds.length > 0 });

  const canCreateInputs = isPermitted(currentUser?.permissions, [
    'inputs:create',
    'input_types:create:org.graylog.collectors.input.CollectorIngestHttpInput',
  ]);

  const readableInputIds = useMemo(
    () => collectorInputIds.filter((id) => isPermitted(currentUser?.permissions, `inputs:read:${id}`)),
    [collectorInputIds, currentUser?.permissions],
  );

  const inputQueries = useQueries({
    queries: readableInputIds.map((id) => ({
      queryKey: ['inputs', id],
      queryFn: () => SystemInputs.get(id),
      retry: false,
    })),
  });

  const allQueriesSettled = inputQueries.every((q) => !q.isLoading);

  const loadedInputs = inputQueries
    .filter((q) => q.isSuccess && q.data)
    .map((q) => q.data);

  const unreadableCount = collectorInputIds.length - readableInputIds.length;
  const hasInputs = loadedInputs.length > 0 || unreadableCount > 0;

  const hasRunningInput = loadedInputs.some((input) => {
    const nodeStates = inputStates?.[input.id];
    if (!nodeStates) return false;

    return Object.values(nodeStates).some((entry) => entry.state === 'RUNNING');
  });

  const handleCreateInput = async () => {
    await createInput({
      input: {
        title: 'Collector Ingest (HTTP)',
        type: 'org.graylog.collectors.input.CollectorIngestHttpInput',
        global: true,
        configuration: {
          bind_address: '0.0.0.0',
          port: defaultPort,
        },
      },
    });
    await queryClient.invalidateQueries({ queryKey: COLLECTOR_INPUT_IDS_KEY_PREFIX });
  };

  if (isLoading || !allQueriesSettled) {
    return null;
  }

  if (isInitialSetup && !hasInputs) {
    return null;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <SectionTitle>Ingest Inputs</SectionTitle>
        {!isInitialSetup && allQueriesSettled && !hasInputs && (
          <Alert bsStyle="info">
            No collector ingest input exists. Collectors will not be able to send data until an input is created.
            {canCreateInputs && (
              <>
                {' '}
                <Button bsSize="xsmall" onClick={handleCreateInput}>
                  Create input
                </Button>
              </>
            )}
          </Alert>
        )}
        {loadedInputs.length === 0 && unreadableCount > 0 && (
          <p>
            {unreadableCount} collector ingest {unreadableCount === 1 ? 'input exists' : 'inputs exist'} but you do
            not have permission to view {unreadableCount === 1 ? 'it' : 'them'}.
          </p>
        )}
        {!isLoadingInputStates && !hasRunningInput && loadedInputs.length > 0 && (
          <Alert bsStyle="info">
            Collector ingest inputs exist but none are currently running. Collectors will not be able to send data.
          </Alert>
        )}
        {loadedInputs.length > 0 && (
          <Table condensed>
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Bind Address</th>
                <th>Port</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loadedInputs.map((input) => (
                <tr key={input.id}>
                  <td>{input.title}</td>
                  <td><InputStateBadge input={input} inputStates={inputStates} /></td>
                  <td>{String(input.attributes?.bind_address ?? '')}</td>
                  <td>{String(input.attributes?.port ?? '')}</td>
                  <td><Link to={`${Routes.SYSTEM.INPUTS}?query=id%3A${input.id}`}>Manage</Link></td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        {unreadableCount > 0 && loadedInputs.length > 0 && (
          <p>
            {unreadableCount} additional {unreadableCount === 1 ? 'input' : 'inputs'} not visible due to
            permissions.
          </p>
        )}
      </Col>
    </Row>
  );
};

export default IngestEndpointStatus;
