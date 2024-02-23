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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Routes from 'routing/Routes';
import { Icon } from 'components/common';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';
import { Link } from 'components/common/router';

type NodeId = string;
type Props = {
  nodeId: NodeId,
};

const BreakWord = styled.span`
  word-break: break-word;
`;

const NodeName = ({ nodeId }: Props) => {
  const node = useStore(NodesStore, (state) => state?.nodes?.[nodeId]);

  if (node) {
    const nodeURL = Routes.node(nodeId);

    return (
      <Link to={nodeURL}>
        <Icon name="lan" />
        &nbsp;
        <BreakWord>
          {node.short_node_id}
        </BreakWord>&nbsp;/&nbsp;
        <BreakWord>
          {node.hostname}
        </BreakWord>
      </Link>
    );
  }

  return <BreakWord>stopped node</BreakWord>;
};

NodeName.propTypes = {
  nodeId: PropTypes.string.isRequired,
};

export default NodeName;
