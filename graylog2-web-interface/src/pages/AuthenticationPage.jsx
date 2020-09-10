// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import AuthenticationServiceDetails from 'components/authentication/AuthenticationServiceDetails';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import { Row, Col } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!
import { PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import AuthenticationComponent from 'components/authentication/AuthenticationComponent';
import ServiceCreateGettingStarted from 'components/authentication/ServiceCreateGettingStarted';

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
};

type Props = {
  location: {
    pathname: string,
  },
  params: {
    name: string,
  },
};

const AuthenticationPage = ({ location, params }: Props) => {
  const [paginatedAuthServices, setPaginatedAuthServices] = useState();

  useEffect(() => {
    AuthenticationActions.loadServicesPaginated(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query).then((newServices) => newServices && setPaginatedAuthServices(newServices));
  }, []);

  if (!paginatedAuthServices) {
    return <Spinner />;
  }

  const activeService = paginatedAuthServices.list.find((service) => service.id === paginatedAuthServices.globalConfig.activeBackend);

  return (
    <>
      <PageHeader title="Authentication Management">
        <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
      </PageHeader>

      <ServiceCreateGettingStarted />

      {activeService && <AuthenticationServiceDetails authenticationService={activeService} />}

      {/* Old authentication management which can be removed soon */}
      <Row className="content">
        <Col md={12}>
          <AuthenticationComponent location={location} params={params} />
        </Col>
      </Row>
    </>
  );
};

AuthenticationPage.propTypes = {
  location: PropTypes.object.isRequired,
  params: PropTypes.object.isRequired,
};

export default AuthenticationPage;
