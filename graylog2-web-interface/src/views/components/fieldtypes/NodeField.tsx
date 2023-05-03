import * as React from 'react';
import { useEffect } from 'react';

import type { ExtractStoreState } from 'stores/connect';
import { useStore } from 'stores/connect';
import { NodesStore, NodesActions } from 'stores/nodes/NodesStore';

type Props = {
  value: string,
}

const mapNodes = (nodeStore: ExtractStoreState<typeof NodesStore>) => nodeStore.nodes;

const NodeField = ({ value }: Props) => {
  useEffect(() => { NodesActions.list(); }, []);
  const nodes = useStore(NodesStore, mapNodes);
  const node = nodes?.[value];

  return node
    ? <span title={value}>{node.short_node_id} / {node.hostname}</span>
    : <span>{value}</span>;
};

export default NodeField;
