import React from 'react';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { DocumentTitle, PageHeader } from 'components/common';
import { NodesList } from 'components/nodes';

const NodesPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Nodes">
        <div>
          <PageHeader title="Nodes">
            <span>This page provides a real-time overview of the nodes in your Graylog cluster.</span>

            <span>
              You can pause message processing at any time. The process buffers will not accept any new messages until
              you resume it. If the message journal is enabled for a node, which it is by default, incoming messages
              will be persisted to disk, even when processing is disabled.
            </span>
          </PageHeader>
          <NodesList permissions={this.state.currentUser.permissions} />
        </div>
      </DocumentTitle>
    );
  },
});

export default NodesPage;
