import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import { Alert, Col, Row } from 'components/graylog';
import { GraylogClusterOverview } from 'components/cluster';
import EnterpriseFreeLicenseForm from 'components/enterprise/EnterpriseFreeLicenseForm';
import PluginList from 'components/enterprise/PluginList';

import CombinedProvider from 'injection/CombinedProvider';

const { EnterpriseActions, EnterpriseStore } = CombinedProvider.get('Enterprise');

const EnterprisePage = createReactClass({
  displayName: 'EnterprisePage',
  mixins: [Reflux.connect(EnterpriseStore)],

  componentDidMount() {
    EnterpriseActions.getLicenseInfo();
  },

  onFreeLicenseFormSubmit(clusterId, formFields, callback) {
    EnterpriseActions.requestFreeEnterpriseLicense(clusterId, formFields)
      .then(() => callback(true))
      .catch(() => callback(false));
  },

  _isLoading() {
    const { clusterId } = this.state;
    return !clusterId;
  },

  renderLicenseFormContent(licenseStatus, clusterId) {
    let licenseFormContent;
    if (this._isLoading()) {
      licenseFormContent = <Spinner text="Loading license status" />;
    } else if (licenseStatus === 'installed') {
      licenseFormContent = (
        <Alert bsStyle="success">
          You already have a Graylog Enterprise license installed.
        </Alert>
      );
    } else if (licenseStatus === 'staged') {
      licenseFormContent = (
        <Alert bsStyle="warning">
          You already requested a free Graylog Enterprise license. It will be activated once you restart the Graylog server with the Graylog Enterprise plugins installed.
        </Alert>
      );
    } else {
      licenseFormContent = <EnterpriseFreeLicenseForm clusterId={clusterId} onSubmit={this.onFreeLicenseFormSubmit} />;
    }

    return licenseFormContent;
  },

  render() {
    console.log('STATE', this.state);
    let orderLink = 'https://www.graylog.org/enterprise';
    const { clusterId, nodeCount, licenseStatus } = this.state;

    if (clusterId) {
      orderLink = `https://www.graylog.org/enterprise?cid=${clusterId}&nodes=${nodeCount}`;
    }

    return (
      <DocumentTitle title="Graylog Enterprise">
        <div>
          <PageHeader title="Graylog Enterprise">
            {null}

            <span>
              Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
              about Graylog Enterprise and order a license on the <a href={orderLink} rel="noopener noreferrer" target="_blank">product page</a>.
            </span>

            <span>
              <a className="btn btn-lg btn-success" href={orderLink} rel="noopener noreferrer" target="_blank">Order a license</a>
            </span>
          </PageHeader>

          <GraylogClusterOverview />
          <PluginList />
          <IfPermitted permissions="freelicenses:create">
            <Row className="content">
              <Col md={12}>
                <h2 style={{ marginBottom: 10 }}>Graylog Enterprise is free for under 5 GB/day</h2>
                {this.renderLicenseFormContent(licenseStatus, clusterId)}
              </Col>
            </Row>
          </IfPermitted>
        </div>
      </DocumentTitle>
    );
  },
});

export default EnterprisePage;
