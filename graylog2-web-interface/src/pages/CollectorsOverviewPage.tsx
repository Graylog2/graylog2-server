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
import { Navigate } from 'react-router-dom';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { CollectorsOverview } from 'components/collectors/overview';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useCollectorsConfig } from 'components/collectors/hooks/useCollectors';
import Routes from 'routing/Routes';

const CollectorsOverviewPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.opamp_ca_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Collectors Overview">
      <CollectorsPageNavigation />
      <PageHeader title="Collectors Overview">
        <span>Overview of all collectors and sources across your infrastructure.</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <CollectorsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsOverviewPage;
