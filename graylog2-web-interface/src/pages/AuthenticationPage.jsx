// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import BackendDetails from 'components/authentication/BackendDetails';
import DocsHelper from 'util/DocsHelper';
import BackendsOverview from 'components/authentication/BackendsOverview';
import {} from 'components/authentication'; // Make sure to load all auth config plugins!
import DocumentationLink from 'components/support/DocumentationLink';
import AuthenticationComponent from 'components/authentication/AuthenticationComponent';
import BackendCreateGettingStarted from 'components/authentication/BackendCreateGettingStarted';
import { PageHeader, Spinner } from 'components/common';
import { Row, Col } from 'components/graylog';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';

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
  const [paginatedAuthBackends, setPaginatedAuthBackends] = useState();

  useEffect(() => {
    AuthenticationActions.loadBackendsPaginated(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query).then((newServices) => newServices && setPaginatedAuthBackends(newServices));
  }, []);

  if (!paginatedAuthBackends) {
    return <Spinner />;
  }

  const activeBackend = paginatedAuthBackends.list.find((backend) => backend.id === paginatedAuthBackends.globalConfig.activeBackend);

  return (
    <>
      <PageHeader title="Authentication Management">
        <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
      </PageHeader>

      <BackendCreateGettingStarted />

      {activeBackend && <BackendDetails authenticationBackend={activeBackend} />}

      {paginatedAuthBackends.list.size >= 1 && (
        <BackendsOverview paginatedAuthBackends={paginatedAuthBackends} />
      )}

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
