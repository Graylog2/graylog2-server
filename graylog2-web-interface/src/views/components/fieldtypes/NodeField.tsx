import * as React from 'react';
import { useContext } from 'react';

import NodesContext from 'contexts/NodesContext';

type Props = {
  value: string,
}

const NodeField = ({ value }: Props) => {
  const nodes = useContext(NodesContext);
  const node = nodes?.[value];

  return node
    ? <span title={value}>{node.short_node_id} / {node.hostname}</span>
    : <span>{value}</span>;
};

export default NodeField;
