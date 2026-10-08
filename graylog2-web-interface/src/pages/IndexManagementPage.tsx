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
import { useEffect } from 'react';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';
import IndicesPageNavigation from 'components/indices/IndicesPageNavigation';
import IndexManagement from 'components/indices/management/IndexManagement';
import useCurrentUser from 'hooks/useCurrentUser';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import { isPermitted } from 'util/PermissionsMixin';

// As ClusterConfigurationPage: the route itself isn't permission-gated, so the page checks for itself.
const IndexManagementPage = () => {
  const currentUser = useCurrentUser();
  const canRead = isPermitted(currentUser.permissions, 'indices:read');
  const { push } = useHistory();

  // Without the permission the page doesn't exist for the user.
  useEffect(() => {
    if (!canRead) {
      push(Routes.NOTFOUND);
    }
  }, [canRead, push]);

  if (!canRead) {
    return null;
  }

  return (
    <DocumentTitle title="Index Management">
      <IndicesPageNavigation />
      <PageHeader title="Index Management">
        <span>
          Every index in the cluster with its health, including hidden indices and indices that don&apos;t belong to a
          Graylog index set.
        </span>
      </PageHeader>
      <IndexerClusterHealth minimal />
      <Row className="content">
        <Col md={12}>
          <IndexManagement />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexManagementPage;
