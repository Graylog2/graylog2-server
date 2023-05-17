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

import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import StreamsContext from 'contexts/StreamsContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import useInputs from 'hooks/useInputs';
import useNodeSummaries from 'hooks/useNodeSummaries';

const formatNode = (node: { short_node_id: string, hostname: string }) => `${node.short_node_id} / ${node.hostname}`;

const useMapKeys = (): KeyMapper => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries(streams?.map((stream) => [stream.id, stream]) ?? []), [streams]);
  const nodes = useNodeSummaries();
  const inputs = useInputs();
  const fieldTypes = useContext(FieldTypesContext);
  const activeQuery = useActiveQueryId();
  const currentFields = useMemo(() => fieldTypes?.queryFields?.get(activeQuery), [activeQuery, fieldTypes?.queryFields]);

  return useCallback((key: Key, field: string) => {
    const fieldType = currentFields?.find((type) => type.name === field);

    switch (fieldType?.type?.type) {
      case 'node':
        return nodes?.[key] ? formatNode(nodes[key]) : key;
      case 'input':
        return inputs?.[key]?.title ?? key;
      case 'streams':
        return streamsMap?.[key]?.title ?? key;
      default:
        return key;
    }
  }, [currentFields, inputs, nodes, streamsMap]);
};

export default useMapKeys;
