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
import CollectorsFleets from 'components/collectors/fleets/CollectorsFleets';
import { useCollectorsConfig } from 'components/collectors/hooks';
import CreateButton from 'components/common/CreateButton';
import Routes from 'routing/Routes';

const CollectorsFleetsPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.signing_cert_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Collector Fleets">
      <CollectorsPageNavigation />
      <PageHeader title={<>Fleets <BetaBadge /></>} actions={<CreateButton entityKey={'Fleet'} />}>
        <span>Manage collector fleets and their configurations.</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <CollectorsFleets />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsFleetsPage;
