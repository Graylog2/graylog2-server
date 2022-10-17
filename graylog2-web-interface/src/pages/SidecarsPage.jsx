/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useEffect, useState } from 'react';

import { Link } from 'components/common/router';
import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import UsersDomain from 'domainActions/users/UsersDomain';
import SidecarListContainer from 'components/sidecars/sidecars/SidecarListContainer';
import Routes from 'routing/Routes';
import SidecarsSubareaNavigation from 'components/sidecars/common/SidecarsSubareaNavigation';
import DocsHelper from 'util/DocsHelper';

const SidecarsPage = () => {
  const [sidecarUser, setSidecarUser] = useState();
  const currentUser = useCurrentUser();
  const canCreateSidecarUserTokens = isPermitted(currentUser?.permissions, ['users:tokenlist:graylog-sidecar']);

  useEffect(() => {
    if (canCreateSidecarUserTokens) {
      UsersDomain.loadByUsername('graylog-sidecar').then(setSidecarUser);
    }
  }, [canCreateSidecarUserTokens]);

  return (
    <DocumentTitle title="Sidecars">
      <SidecarsSubareaNavigation />
      <PageHeader title="Sidecars Overview"
                  documentationLink={{
                    title: 'Sidecar documentation',
                    path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                  }}>
        <span>
          The Graylog sidecars can reliably forward contents of log files or Windows EventLog from your servers.
          {canCreateSidecarUserTokens && (
            sidecarUser ? (
              <span>
                <br />
                Do you need an API token for a sidecar?&ensp;
                <Link to={Routes.SYSTEM.USERS.TOKENS.edit(sidecarUser.id)}>
                  Create or reuse a token for the <em>graylog-sidecar</em> user
                </Link>
              </span>
            ) : <Spinner />
          )}
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <SidecarListContainer />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default SidecarsPage;
