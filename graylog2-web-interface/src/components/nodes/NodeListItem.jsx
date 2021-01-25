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
import React from 'react';

import { Col } from 'components/graylog';
import { EntityListItem, LinkToNode } from 'components/common';
import NodeThroughput from 'components/throughput/NodeThroughput';

import NodesActions from './NodesActions';
import SystemOverviewSummary from './SystemOverviewSummary';
import JvmHeapUsage from './JvmHeapUsage';
import JournalState from './JournalState';

class NodeListItem extends React.Component {
  static propTypes = {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object,
  };

  render() {
    const { node } = this.props;
    const title = <LinkToNode nodeId={node.node_id} />;

    if (!this.props.systemOverview) {
      return (
        <EntityListItem key={`entry-list-${node.node_id}`}
                        title={title}
                        description="System information is currently unavailable." />
      );
    }

    const nodeThroughput = <NodeThroughput nodeId={node.node_id} />;
    const journalState = <JournalState nodeId={node.node_id} />;
    const actions = <NodesActions node={node} systemOverview={this.props.systemOverview} />;

    const additionalContent = (
      <div>
        <Col md={3}>
          <SystemOverviewSummary information={this.props.systemOverview} />
        </Col>
        <Col md={9}>
          <JvmHeapUsage nodeId={this.props.node.node_id} />
        </Col>
      </div>
    );

    return (
      <EntityListItem key={`entry-list-${node.node_id}`}
                      title={title}
                      titleSuffix={nodeThroughput}
                      description={journalState}
                      actions={actions}
                      contentRow={additionalContent} />
    );
  }
}

export default NodeListItem;
