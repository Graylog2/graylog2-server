import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Row, Col } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import AuthenticationComponent from 'components/authentication/AuthenticationComponent';

const AuthenticationPage = createReactClass({
  displayName: 'AuthenticationPage',

  propTypes: {
    children: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    params: PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],

  render() {
    const { location, params, children } = this.props;

    return (
      <span>
        <PageHeader title="Authentication Management">
          <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <AuthenticationComponent location={location} params={params}>
              {children}
            </AuthenticationComponent>
          </Col>
        </Row>
      </span>
    );
  },
});

export default AuthenticationPage;
