import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';

import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import { Alert, Col, Row } from 'components/graylog';
import { GraylogClusterOverview } from 'components/cluster';
import DocumentationLink from 'components/support/DocumentationLink';
import EnterpriseFreeLicenseForm from 'components/enterprise/EnterpriseFreeLicenseForm';
import PluginList from 'components/enterprise/PluginList';

import CombinedProvider from 'injection/CombinedProvider';

const { EnterpriseActions, EnterpriseStore } = CombinedProvider.get('Enterprise');

const EnterpriseProductLink = ({ children }) => {
  return (
    <a href="https://www.graylog.org/products/enterprise"
       rel="noopener noreferrer"
       target="_blank">
      {children}
    </a>
  );
};

EnterpriseProductLink.propTypes = {
  children: PropTypes.node,
};
EnterpriseProductLink.defaultProps = {
  children: null,
};

const EnterpriseFeatureList = styled.ul`
list-style-type: disc;
padding-left: 20px;
`;

const BiggerFontSize = styled.div`
font-size: 1.2em;
`;

const GraylogEnterpriseHeader = styled.h2`
margin-bottom: 10px
`;

const EnterprisePage = createReactClass({
  displayName: 'EnterprisePage',
  mixins: [Reflux.connect(EnterpriseStore)],

  componentDidMount() {
    EnterpriseActions.getLicenseInfo();
  },

  onFreeLicenseFormSubmit(formFields, callback) {
    EnterpriseActions.requestFreeEnterpriseLicense(formFields)
      .then(() => callback(true))
      .catch(() => callback(false));
  },

  _isLoading() {
    const { licenseStatus } = this.state;
    return !licenseStatus;
  },

  renderLicenseFormContent(licenseStatus) {
    let licenseFormContent;
    if (this._isLoading()) {
      licenseFormContent = <Spinner text="Loading license status" />;
    } else if (licenseStatus === 'installed') {
      licenseFormContent = (
        <Alert bsStyle="success">
          You have a Graylog Enterprise license installed.
        </Alert>
      );
    } else if (licenseStatus === 'staged') {
      licenseFormContent = (
        <Alert bsStyle="warning">
          You requested a free Graylog Enterprise license. It will be activated once you restart the Graylog
          server with the Graylog Enterprise plugins installed.
        </Alert>
      );
    } else {
      licenseFormContent = <EnterpriseFreeLicenseForm onSubmit={this.onFreeLicenseFormSubmit} />;
    }

    return licenseFormContent;
  },

  render() {
    const { licenseStatus } = this.state;

    return (
      <DocumentTitle title="Try Graylog Enterprise">
        <div>
          <PageHeader title="Try Graylog Enterprise">
            {null}

            <span>
              Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
              about Graylog Enterprise on the <EnterpriseProductLink>product page</EnterpriseProductLink>.
            </span>
          </PageHeader>

          <GraylogClusterOverview layout="compact">
            <PluginList />
          </GraylogClusterOverview>
          <IfPermitted permissions="freelicenses:create">
            <Row className="content">
              <Col md={6}>
                <GraylogEnterpriseHeader>Graylog Enterprise</GraylogEnterpriseHeader>
                <BiggerFontSize>
                  <p><strong>Extend Graylog’s Open Source capabilities with a free trial of Graylog Enterprise for 30 days.</strong></p>
                  <p>
                    Graylog Enterprise introduces productivity and compliance features designed to help organizations
                    reduce risk while encouraging collaboration across a large number of users.
                  </p>

                  <p>Graylog Enterprise includes:</p>

                  <EnterpriseFeatureList>
                    <li>Automated <DocumentationLink page="archiving.html" text={<strong>archiving</strong>} /> and retention</li>
                    <li><DocumentationLink page="auditlog.html" text={<strong>Audit logs</strong>} /> of Graylog user activity</li>
                    <li>
                      Alerts with <DocumentationLink page="alerts.html#filter-with-dynamic-lists-enterprise-feature"
                                                     text={<strong>dynamic lists</strong>} /> and <DocumentationLink
                      page="alerts.html" text={<strong>correlation engine</strong>} /> for events
                      to minimize the number of alerts that you need to create and maintain
                    </li>
                    <li>
                      Customizable <DocumentationLink page="reporting.html" text={<strong>scheduled reporting</strong>} /> using dashboard widgets for sharing analysis outside Graylog
                    </li>
                    <li><DocumentationLink page="searching/parameters.html"
                                           text={<strong>Parameterized search templates</strong>} /> enable you to
                      combine and reuse queries
                    </li>
                    <li><DocumentationLink page="integrations/forwarder.html"
                                           text={<strong>Data forwarder</strong>} /> to easily combine data from
                      multiple Graylog instances
                    </li>
                    <li>And more...</li>
                  </EnterpriseFeatureList>
                </BiggerFontSize>
              </Col>
              <Col md={6}>
                {this.renderLicenseFormContent(licenseStatus)}
              </Col>
            </Row>
          </IfPermitted>
        </div>
      </DocumentTitle>
    );
  },
});

export default EnterprisePage;
