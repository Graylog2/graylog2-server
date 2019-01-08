import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader } from 'components/common';
import { GraylogClusterOverview } from 'components/cluster';
import PluginList from 'components/enterprise/PluginList';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');

const EnterprisePage = createReactClass({
  displayName: 'EnterprisePage',
  mixins: [Reflux.connect(NodesStore)],

  _isLoading() {
    return !this.state.nodes;
  },

  render() {
    let orderLink = 'https://www.graylog.org/enterprise';

    if (this.state.clusterId) {
      orderLink = `https://www.graylog.org/enterprise?cid=${this.state.clusterId}&nodes=${this.state.nodeCount}`;
    }

    return (
      <DocumentTitle title="Graylog Enterprise">
        <div>
          <PageHeader title="Graylog Enterprise">
            {null}

            <span>
              Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
              about Graylog Enterprise and order a license on the <a href={orderLink} target="_blank">product page</a>.
            </span>

            <span>
              <a className="btn btn-lg btn-success" href={orderLink} target="_blank">Order a license</a>
            </span>
          </PageHeader>

          <GraylogClusterOverview />
          <PluginList />
        </div>
      </DocumentTitle>
    );
  },
});

export default EnterprisePage;
