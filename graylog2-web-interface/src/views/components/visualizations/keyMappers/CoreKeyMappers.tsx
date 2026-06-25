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
import { useCallback, useContext, useMemo } from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import StreamsContext from 'contexts/StreamsContext';
import { useInputs } from 'hooks/useInputs';
import useNodeSummaries from 'hooks/useNodeSummaries';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';

const formatNode = (node: { short_node_id: string; hostname: string }) => `${node.short_node_id} / ${node.hostname}`;

const useStreamsKeyMapper = () => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries((streams ?? []).map((stream) => [stream.id, stream])), [streams]);

  return useCallback((key: Key) => streamsMap[key]?.title ?? key, [streamsMap]);
};

const useInputKeyMapper = () => {
  const inputs = useInputs();

  return useCallback((key: Key) => inputs?.[key]?.title ?? key, [inputs]);
};

const useNodeKeyMapper = () => {
  const nodes = useNodeSummaries();

  return useCallback((key: Key) => (nodes?.[key] ? formatNode(nodes[key]) : key), [nodes]);
};

const CoreKeyMappers: PluginExports['visualizationKeyMappers'] = [
  { type: 'streams', useKeyMapper: useStreamsKeyMapper },
  { type: 'input', useKeyMapper: useInputKeyMapper },
  { type: 'node', useKeyMapper: useNodeKeyMapper },
];

export default CoreKeyMappers;
