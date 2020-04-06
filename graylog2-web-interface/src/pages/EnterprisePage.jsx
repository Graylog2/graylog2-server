import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import { GraylogClusterOverview } from 'components/cluster';
import EnterpriseFreeLicenseForm from 'components/enterprise/EnterpriseFreeLicenseForm';
import PluginList from 'components/enterprise/PluginList';

import StoreProvider from 'injection/StoreProvider';
import CombinedProvider from 'injection/CombinedProvider';

const NodesStore = StoreProvider.getStore('Nodes');
const { EnterpriseActions, EnterpriseStore } = CombinedProvider.get('Enterprise');

const EnterprisePage = createReactClass({
  displayName: 'EnterprisePage',
  mixins: [Reflux.connect(NodesStore), Reflux.connect(EnterpriseStore)],

  onFreeLicenseFormSubmit(clusterId, formFields, callback) {
    EnterpriseActions.requestFreeEnterpriseLicense(clusterId, formFields)
      .then(() => callback(true))
      .catch(() => callback(false));
  },

  _isLoading() {
    return !this.state.nodes;
  },

  render() {
    let orderLink = 'https://www.graylog.org/enterprise';
    const { clusterId, nodeCount } = this.state;

    if (clusterId) {
      orderLink = `https://www.graylog.org/enterprise?cid=${clusterId}&nodes=${nodeCount}`;
    }

    // TODO: Only render form if there is no license installed.

    return (
      <DocumentTitle title="Graylog Enterprise">
        <div>
          <PageHeader title="Graylog Enterprise">
            {null}

            <span>
              Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
              about Graylog Enterprise and order a license on the <a href={orderLink} rel="noopener noreferrer"
                                                                     target="_blank">product page</a>.
            </span>

            <span>
              <a className="btn btn-lg btn-success" href={orderLink} rel="noopener noreferrer" target="_blank">Order a license</a>
            </span>
          </PageHeader>

          <GraylogClusterOverview />
          <PluginList />
          <IfPermitted permissions="freelicenses:create">
            <EnterpriseFreeLicenseForm clusterId={clusterId} onSubmit={this.onFreeLicenseFormSubmit} />
          </IfPermitted>
        </div>
      </DocumentTitle>
    );
  },
});

export default EnterprisePage;
