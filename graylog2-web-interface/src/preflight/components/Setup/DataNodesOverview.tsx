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
import { Space } from '@mantine/core';
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { Alert, Badge, List, Button } from 'preflight/components/common';

const P = styled.p`
  max-width: 700px;
`;

const NodeId = styled(Badge)`
  margin-right: 3px;
`;

type Props = {
  onResumeStartup: () => void
}

const DataNodesOverview = ({ onResumeStartup }: Props) => {
  const {
    data: dataNodes,
    isFetching: isFetchingDataNodes,
    error: dataNodesFetchError,
    isInitialLoading: isInitialLoadingDataNodes,
  } = useDataNodes();

  return (
    <>
      <P>
        Graylog data nodes offer a better integration with Graylog and simplify future updates.
        Once a Graylog data node is running, you can click on &quot;Resume startup&quot;.
      </P>
      <P>
        These are the data nodes which are currently registered.
        The list is constantly updated. {isFetchingDataNodes && <Spinner text="" />}
      </P>

      {!!dataNodes.length && (
        <>
          <Space h="sm" />
          <List spacing="xs">
            {dataNodes.map(({
              hostname,
              transport_address,
              short_node_id,
            }) => (
              <List.Item key={short_node_id}>
                <NodeId title="Short node id">{short_node_id}</NodeId>
                <span title="Transport address">{transport_address}</span>{' – '}
                <span title="Hostname">{hostname}</span>
              </List.Item>
            ))}
          </List>
        </>
      )}
      {(!dataNodes.length && !isInitialLoadingDataNodes) && (
        <Alert type="info">
          No data nodes have been found.
        </Alert>
      )}
      {dataNodesFetchError && (
        <Alert type="danger">
          There was an error fetching the data nodes: {dataNodesFetchError.message}
        </Alert>
      )}
      <Space h="md" />
      <Button onClick={onResumeStartup} size="xs">
        Resume startup
      </Button>
    </>
  );
};

export default DataNodesOverview;
