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
import { useEffect, useState, useContext } from 'react';

import { LinkContainer, Link } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import CurrentUserContext from 'contexts/CurrentUserContext';
import UsersDomain from 'domainActions/users/UsersDomain';
import SidecarListContainer from 'components/sidecars/sidecars/SidecarListContainer';
import Routes from 'routing/Routes';

const SidecarsPage = () => {
  const [sidecarUser, setSidecarUser] = useState();
  const currentUser = useContext(CurrentUserContext);
  const canCreateSidecarUserTokens = isPermitted(currentUser?.permissions, ['users:tokenlist:graylog-sidecar']);

  useEffect(() => {
    if (canCreateSidecarUserTokens) {
      UsersDomain.loadByUsername('graylog-sidecar').then(setSidecarUser);
    }
  }, [canCreateSidecarUserTokens]);

  return (
    <DocumentTitle title="Sidecars">
      <span>
        <PageHeader title="Sidecars Overview">
          <span>
            The Graylog sidecars can reliably forward contents of log files or Windows EventLog from your servers.
          </span>

          {canCreateSidecarUserTokens && (
            <>
              {sidecarUser ? (
                <span>
                  Do you need an API token for a sidecar?&ensp;
                  <Link to={Routes.SYSTEM.USERS.TOKENS.edit(sidecarUser.id)}>
                    Create or reuse a token for the <em>graylog-sidecar</em> user
                  </Link>
                </span>
              ) : <Spinner />}
            </>
          )}

          {!canCreateSidecarUserTokens && <></>}

          <ButtonToolbar>
            <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
              <Button bsStyle="info">Overview</Button>
            </LinkContainer>
            <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
              <Button bsStyle="info">Administration</Button>
            </LinkContainer>
            <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
              <Button bsStyle="info">Configuration</Button>
            </LinkContainer>
          </ButtonToolbar>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <SidecarListContainer />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default SidecarsPage;
