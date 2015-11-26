import React from 'react';
import Reflux from 'reflux';

import NodesStore from 'stores/nodes/NodesStore';

import { PageHeader, Spinner } from 'components/common';
import { SmallSupportLink } from 'components/support';

const ShowNodePage = React.createClass({
  mixins: [Reflux.connect(NodesStore)],
  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }
    const nodeId = this.props.params.nodeId;
    const node = this.state.nodes[nodeId];

    return (
      <PageHeader title="Node">
        <span>
          This page shows details of a Graylog server node that is active and reachable in your cluster.
        </span>
        <span>
          {node.is_master && <span>This is the master node.</span>}
        </span>
      </PageHeader>
    );
  },
});

export default ShowNodePage;
