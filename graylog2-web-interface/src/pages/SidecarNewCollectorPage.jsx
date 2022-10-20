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
import React from 'react';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import CollectorForm from 'components/sidecars/configuration-forms/CollectorForm';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';

const SidecarNewCollectorPage = () => (
  <DocumentTitle title="New Log Collector">
    <SidecarsPageNavigation />
    <PageHeader title="New Log Collector">
      <span>
        Some words about log collectors.
      </span>

      <span>
        Read more about the Graylog Sidecar in the documentation.
      </span>
    </PageHeader>

    <Row className="content">
      <Col md={6}>
        <CollectorForm action="create" />
      </Col>
    </Row>
  </DocumentTitle>
);

export default SidecarNewCollectorPage;
