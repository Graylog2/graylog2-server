import React from 'react';
import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';

import {} from 'components/authentication'; // Make sure to load all auth config plugins!

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';

import AuthenticationComponent from 'components/authentication/AuthenticationComponent';

const AuthenticationPage = React.createClass({

  propTypes: {
    children: React.PropTypes.object,
    location: React.PropTypes.object.isRequired,
    params: React.PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],

  render() {
    return (
      <span>
        <PageHeader title="Authentication Management">
          <span>Configure Graylog's authentication providers and manage the active users of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation"/>.</span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <AuthenticationComponent location={this.props.location} params={this.props.params}>
              {this.props.children}
            </AuthenticationComponent>
          </Col>
        </Row>
      </span>
    );
  },
});

export default AuthenticationPage;
