import React, { PropTypes } from 'react';
import { Col } from 'react-bootstrap';

import { EntityListItem, LinkToNode } from 'components/common';
import NodesActions from './NodesActions';
import SystemOverviewSummary from './SystemOverviewSummary';
import JvmHeapUsage from './JvmHeapUsage';
import JournalState from './JournalState';
import NodeThroughput from 'components/throughput/NodeThroughput';

const NodeListItem = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object,
  },
  render() {
    const node = this.props.node;
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
  },
});

export default NodeListItem;
