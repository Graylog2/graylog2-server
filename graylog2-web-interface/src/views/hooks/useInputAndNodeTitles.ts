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
import { useCallback, useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';

import { useInputs } from 'hooks/useInputs';
import useNodeSummaries from 'hooks/useNodeSummaries';
import usePluginEntities from 'hooks/usePluginEntities';
import { MISSING_BUCKET_NAME } from 'views/Constants';

type Titles = { [id: string]: string };
type TitleResolver = (id: string) => string | undefined;

// Query keys are shared across all consumers, so forwarder lookups are cached app-wide.
const useForwarderTitles = (
  kind: 'input' | 'node',
  ids: Array<string>,
  fetchFn: ((id: string) => Promise<{ title: string }>) | undefined,
): Titles =>
  useQueries({
    queries: ids.map((id) => ({
      queryKey: ['forwarder', kind, id],
      queryFn: () => fetchFn(id),
      enabled: !!fetchFn,
      retry: false,
    })),
    combine: (results): Titles =>
      Object.fromEntries(ids.map((id, idx) => [id, results[idx].data?.title]).filter(([, title]) => !!title)),
  });

const useUnresolvedIds = (ids: Array<string>, known: { [id: string]: unknown } | undefined) =>
  useMemo(
    () => (known ? ids.filter((id) => id && id !== MISSING_BUCKET_NAME && !known[id]) : []),
    [ids, known],
  );

export const useInputTitleResolver = (ids: Array<string>): TitleResolver => {
  const inputs = useInputs();
  const { fetchForwarderInput } = usePluginEntities('forwarder')[0] ?? {};
  const forwarderTitles = useForwarderTitles('input', useUnresolvedIds(ids, inputs), fetchForwarderInput);

  return useCallback((id: string) => inputs?.[id]?.title ?? forwarderTitles[id], [inputs, forwarderTitles]);
};

const formatNode = (node: { short_node_id: string; hostname: string }) => `${node.short_node_id} / ${node.hostname}`;

export const useNodeTitleResolver = (ids: Array<string>): TitleResolver => {
  const nodes = useNodeSummaries();
  const { fetchForwarderNode } = usePluginEntities('forwarder')[0] ?? {};
  const forwarderTitles = useForwarderTitles('node', useUnresolvedIds(ids, nodes), fetchForwarderNode);

  return useCallback(
    (id: string) => (nodes?.[id] ? formatNode(nodes[id]) : forwarderTitles[id]),
    [nodes, forwarderTitles],
  );
};
