import { useCallback, useContext, useMemo } from 'react';

import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import StreamsContext from 'contexts/StreamsContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import NodesContext from 'contexts/NodesContext';
import InputsContext from 'contexts/InputsContext';
import type { NodeInfo } from 'stores/nodes/NodesStore';

const formatNode = (node: NodeInfo) => `${node.short_node_id} / ${node.hostname}`;

const useMapKeys = (): KeyMapper => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries(streams?.map((stream) => [stream.id, stream]) ?? []), [streams]);
  const nodes = useContext(NodesContext);
  const inputs = useContext(InputsContext);
  const fieldTypes = useContext(FieldTypesContext);
  const activeQuery = useActiveQueryId();
  const currentFields = useMemo(() => fieldTypes?.queryFields?.get(activeQuery), [activeQuery, fieldTypes?.queryFields]);

  return useCallback((key: Key, field: string) => {
    const fieldType = currentFields.find((type) => type.name === field);

    switch (fieldType?.type?.type) {
      case 'node':
        return nodes[key] ? formatNode(nodes[key]) : key;
      case 'input':
        return inputs[key]?.title ?? key;
      case 'streams':
        return streamsMap[key]?.title ?? key;
      default:
        return key;
    }
  }, [currentFields, inputs, nodes, streamsMap]);
};

export default useMapKeys;
