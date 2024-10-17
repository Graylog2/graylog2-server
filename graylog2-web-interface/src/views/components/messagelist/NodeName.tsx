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
import styled from 'styled-components';

import Routes from 'routing/Routes';
import { Icon } from 'components/common';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';
import { Link } from 'components/common/router';
import AppConfig from 'util/AppConfig';

type NodeId = string;

const BreakWord = styled.span`
  word-break: break-word;
`;

type NodeTitleProps = {
  shortNodeId: string,
  hostname: string
}

const NodeTitle = ({ shortNodeId, hostname }: NodeTitleProps) => (
  <>
    <Icon name="lan" />
    &nbsp;
    <BreakWord>
      {shortNodeId}
    </BreakWord>&nbsp;/&nbsp;
    <BreakWord>
      {hostname}
    </BreakWord>
  </>
);

type Props = {
  nodeId: NodeId,
};

const NodeName = ({ nodeId }: Props) => {
  const node = useStore(NodesStore, (state) => state?.nodes?.[nodeId]);

  if (!node) {
    return <BreakWord>stopped node</BreakWord>;
  }

  if (AppConfig.isCloud()) {
    return <NodeTitle shortNodeId={node.short_node_id} hostname={node.hostname} />;
  }

  return (
    <Link to={Routes.node(nodeId)}>
      <NodeTitle shortNodeId={node.short_node_id} hostname={node.hostname} />
    </Link>
  );
};

export default NodeName;
