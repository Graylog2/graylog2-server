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
import { useQuery } from '@tanstack/react-query';

import useNodeSummaries from 'hooks/useNodeSummaries';
import usePluginEntities from 'hooks/usePluginEntities';

type Props = {
  value: string,
}

const useForwarderNode = (nodeId: string, enabled: boolean) => {
  const { fetchForwarderNode } = usePluginEntities('forwarder')[0] ?? {};
  const { data: forwarderNode, isError, isLoading } = useQuery(
    ['forwarder', 'node', nodeId],
    () => fetchForwarderNode(nodeId),
    { enabled: fetchForwarderNode && enabled },
  );

  return (isLoading || isError) ? undefined : forwarderNode;
};

const NodeField = ({ value }: Props) => {
  const nodes = useNodeSummaries();
  const node = nodes?.[value];
  const forwarderNode = useForwarderNode(value, nodes && !node);
  const nodeTitle = (node ? `${node.short_node_id} / ${node.hostname}` : forwarderNode?.title) ?? value;

  return <span title={value}>{nodeTitle}</span>;
};

export default NodeField;
