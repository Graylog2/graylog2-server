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
import { useMemo } from 'react';

import { Row, Col } from 'components/bootstrap';
import { Spinner, EntityList, Pluralize } from 'components/common';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { useStore } from 'stores/connect';

import NodeListItem from './NodeListItem';

type Props = {
  nodes?: {},
}

const NodesList = ({ nodes }: Props) => {
  const { clusterOverview } = useStore(ClusterOverviewStore);

  const _isLoading = !nodes || !clusterOverview;

  const _formattedNodes = useMemo(() => {
    if (_isLoading) {
      return [];
    }

    return Object.keys(nodes).map((nodeID) => <NodeListItem key={nodeID} node={nodes[nodeID]} systemOverview={clusterOverview[nodeID]} />);
  }, [clusterOverview, nodes]);

  if (_isLoading) {
    return <Spinner />;
  }

  const nodesNo = Object.keys(nodes).length;

  return (
    <Row className="content">
      <Col md={12}>
        <h2>
          There <Pluralize value={nodesNo} singular="is" plural="are" /> {nodesNo} active <Pluralize value={nodesNo} singular="node" plural="nodes" />
        </h2>
        <EntityList bsNoItemsStyle="info"
                    noItemsText="There are no active nodes."
                    items={_formattedNodes} />
      </Col>
    </Row>
  );
};

export default NodesList;
