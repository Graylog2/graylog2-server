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
import BetaBadge from 'components/common/BetaBadge';
import { CollectorsPageNavigation } from 'components/collectors/common';
import CollectorsInstances from 'components/collectors/instances/CollectorsInstances';
import { useCollectorsConfig } from 'components/collectors/hooks';
import Routes from 'routing/Routes';

const CollectorsInstancesPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.signing_cert_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Collector Instances">
      <CollectorsPageNavigation />
      <PageHeader title={<>Instances <BetaBadge /></>}>
        <span>View all collector instances across fleets.</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <CollectorsInstances />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsInstancesPage;
