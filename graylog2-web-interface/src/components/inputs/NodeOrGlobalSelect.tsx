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
import PropTypes from 'prop-types';
import React, { useCallback, useState, useEffect } from 'react';

import { useStore } from 'stores/connect';
import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import { Spinner } from 'components/common';

const NodesStore = StoreProvider.getStore('Nodes');

type Props = {
  global: boolean,
  node: string,
  onChange: (type: 'node' | 'global', value: boolean | string | undefined | null) => void,
};

const NodeOrGlobalSelect = ({ global = false, node, onChange }: Props) => {
  const { nodes } = useStore(NodesStore);
  const [globalState, setGlobal] = useState(global);
  const [nodeState, setNode] = useState(node);

  useEffect(() => {
    if (!node && nodes) {
      const nodeIds = Object.keys(nodes);

      if (nodeIds.length === 1) {
        onChange('node', nodeIds[0]);
        setNode(nodeIds[0]);
      }
    }
  }, [nodes]);

  const _onChangeGlobal = useCallback((evt) => {
    const isGlobal = evt.target.checked;

    setGlobal(isGlobal);

    if (isGlobal) {
      setNode('placeholder');
      onChange('node', undefined);
    } else {
      onChange('node', nodeState);
    }

    onChange('global', isGlobal);
  }, [onChange, nodeState, setNode, setGlobal]);

  const _onChangeNode = useCallback((evt) => {
    setNode(evt.target.value);
    onChange('node', evt.target.value);
  }, [setNode, onChange]);

  if (!nodes) {
    return <Spinner />;
  }

  const options = Object.keys(nodes)
    .map((nodeId) => {
      return <option key={nodeId} value={nodeId}>{nodes[nodeId].short_node_id} / {nodes[nodeId].hostname}</option>;
    });

  const nodeSelect = !globalState ? (
    <Input id="node-select"
           type="select"
           label="Node"
           placeholder="placeholder"
           value={node}
           help="On which node should this input start"
           onChange={_onChangeNode}
           required>
      <option key="placeholder" value="">Select Node</option>
      {options}
    </Input>
  ) : null;

  return (
    <span>
      <Input id="global-checkbox"
             type="checkbox"
             label="Global"
             help="Should this input start on all nodes"
             checked={globalState}
             onChange={_onChangeGlobal} />
      {nodeSelect}
    </span>
  );
};

NodeOrGlobalSelect.propTypes = {
  global: PropTypes.bool,
  onChange: PropTypes.func.isRequired,
  node: PropTypes.string,
};

NodeOrGlobalSelect.defaultProps = {
  global: false,
  node: undefined,
};

export default NodeOrGlobalSelect;
