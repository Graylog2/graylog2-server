import PropTypes from 'prop-types';
import React from 'react';

import { Col, Row } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import AuthenticationComponent from 'components/authentication/AuthenticationComponent';
import {} from 'components/authentication';

const AuthenticationPage = ({ params, location, children }) => (
  <span>
    <PageHeader title="Authentication Management">
      <span>Configure Graylog&apos;s authentication providers and manage the active users of this Graylog cluster.</span>
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

AuthenticationPage.propTypes = {
  children: PropTypes.object,
  location: PropTypes.object.isRequired,
  params: PropTypes.object.isRequired,
};

AuthenticationPage.defaultProps = {
  children: undefined,
};

export default withParams(withLocation(AuthenticationPage));
