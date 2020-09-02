// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Row, Col } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import AuthenticationComponent from 'components/authentication/AuthenticationComponent';
import { EmptyEntity } from 'components/common';

type Props = {
  location: {
    pathname: string,
  },
  params: {
    name: string,
  },
};

const AuthenticationPage = ({ location, params }: Props) => (
  <>
    <PageHeader title="Authentication Management">
      <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
      <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                               text="documentation" />.
      </span>
    </PageHeader>

    <Row className="content">
      <Col xs={8} xsOffset={2} md={6} mdOffset={3}>
        <EmptyEntity>
          <p>
            Beside the builtin authentication mechanisms like its internal user database, or LDAP/Active Directory,
            authentication provider can also be extended by plugins to support other authentication mechanisms, for example Single Sign-On or Two Factor Authentication.
            Select an authentication provider to configure a new one.
          </p>
        </EmptyEntity>
      </Col>
    </Row>

    {/* Old authentication management which can be removed soon */}
    <Row className="content">
      <Col md={12}>
        <AuthenticationComponent location={location} params={params} />
      </Col>
    </Row>
  </>
);

AuthenticationPage.propTypes = {
  location: PropTypes.object.isRequired,
  params: PropTypes.object.isRequired,
};

export default AuthenticationPage;
